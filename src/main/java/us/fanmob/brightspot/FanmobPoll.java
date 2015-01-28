package us.fanmob.brightspot;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Renderer;
import com.psddev.cms.db.ToolUi;

import com.psddev.dari.db.Application;
import com.psddev.dari.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToolUi.LabelFields("question")
@ToolUi.Referenceable
@ToolUi.Note("Once you've created this poll, you will not be able to edit it! Instead, create and embed a new poll.")
public class FanmobPoll extends Content implements Renderer, FanMobObject {
    @ToolUi.Hidden
    private String fanmobId;

    @ToolUi.Hidden
    private String webUrl;

    @Required
    @Maximum(120)
    @ToolUi.SuggestedMaximum(120)
    private String question;

    @CollectionMinimum(2)
    @CollectionMaximum(6)
    @Maximum(60)
    @ToolUi.SuggestedMaximum(60)
    @ToolUi.Note("At least two choices to vote on.")
    private List<String> choices;

    @Required
    @ToolUi.Note("Topics allow for filtering in widgets.")
    private Set<FanmobTopic> topics;

    public String getFanmobId() {
        return this.fanmobId;
    }

    public String getQuestion() {
        return this.question;
    }

    public String getWebUrl() {
        return this.webUrl;
    }

    public String getWebUrlBase() {
        try {
            URL url = new URL(this.webUrl);
            return url.getProtocol() + "://" + url.getHost();
        } catch (Exception e) {
            return "https://www.fanmob.us";
        }
    }

    private static class PollCreationRequest {
        public static class Poll {
            public String question;
            public List<String> choices;
            public List<String> topics;
        }

        public Poll poll;

        PollCreationRequest(String q, List<String> choices, Set<FanmobTopic> topics) {
            Poll p = new Poll();
            p.question = q;
            p.choices = choices;
            p.topics = new ArrayList<String>();
            for (FanmobTopic t: topics) {
                p.topics.add(t.getName());
            }
            this.poll = p;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PollCreationResponse {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Poll {
            public String id;
            @JsonProperty("web_url")
            public String webUrl;
        }

        public Poll poll;

        public String getPollId() {
            return this.poll.id;
        }

        public String getWebUrl() {
            return this.poll.webUrl;
        }
    }

    @Override
    // Aren't using @Renderer.Path pointing at a JSP because I cannot figure out
    // how to corectly reference them from a JAR
    public void renderObject(
            HttpServletRequest request,
            HttpServletResponse response,
            HtmlWriter writer)
            throws IOException {
        writer.writeStart("script");
        writer.writeRaw("(function(d, s, id, or) { var js, fjs = d.getElementsByTagName(s)[0]; if (d.getElementById(id)) return; js = d.createElement(s); js.id = id; js.src = or + '/assets/sdk.js'; fjs.parentNode.insertBefore(js, fjs); }");
        writer.writeRaw("(document,'script','fnmb-jssdk','" + this.getWebUrlBase() + "'));");
        writer.writeEnd();

        writer.writeStart("a", "href", this.getWebUrl(), "_target", "blank",
                "data-fnmb-embed", "poll", "data-fnmb-id", this.getFanmobId());
        writer.writeHtml(this.getQuestion());
        writer.writeEnd();
    }

    @Override
    public void create() {
        Logger logger = LoggerFactory.getLogger(FanmobPoll.class);

        // Returns the URL of the created poll
        ResponseHandler<PollCreationResponse> responseHandler = new ResponseHandler<PollCreationResponse>() {
            public PollCreationResponse handleResponse(final HttpResponse response)
                    throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();

                if (status >= 200 && status < 300) {
                    Logger logger = LoggerFactory.getLogger(FanmobPoll.class);
                    String json = EntityUtils.toString(response.getEntity());
                    logger.debug("FanMob poll creation response: " + json);

                    ObjectMapper mapper = new ObjectMapper();
                    PollCreationResponse pcr = mapper.readValue(json, PollCreationResponse.class);
                    return pcr;
                } else if (status == 401) {
                    throw new ClientProtocolException("FanMob API: Unauthorized (check [fanmob/apiKey] setting)");
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        FanmobSettings settings = Application.Static.getInstance(FanmobSettings.class);

        if (settings == null) {
            throw new IllegalArgumentException("No Fanmob Settings found");
        }

        String apiKey = settings.getApiKey();
        String baseUrl = settings.getApiUrl();

        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("Fanmob ApiKey not found in settings");
        }

        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("Fanmob base url not found in settings");
        }

        HttpPost post = new HttpPost(baseUrl + "/polls");
        post.setHeader("User-Agent", "FanMob-Brightspot/0.1.0");
        post.setHeader("Authorization", "Token " + apiKey);
        post.setHeader("Content-type", "application/json");

        PollCreationRequest req = new PollCreationRequest(this.question, this.choices, this.topics);
        ObjectMapper mapper = new ObjectMapper();

        try {
            String json = mapper.writeValueAsString(req);
            logger.debug("Sending JSON: " + json);
            post.setEntity(new StringEntity(json));

            logger.debug("Executing FanMob API request " + post.getRequestLine());
            HttpClient client = new DefaultHttpClient();
            PollCreationResponse pcr = client.execute(post, responseHandler);
            logger.debug("Got a poll with ID: " + pcr.getPollId() + " at: " + pcr.getWebUrl());
            this.webUrl = pcr.getWebUrl();
            this.fanmobId = pcr.getPollId();
        } catch (IOException e) {
            this.as(FanMobObject.Status.class).setError(e.getMessage());
        }
    }
}
