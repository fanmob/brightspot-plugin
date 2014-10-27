package us.fanmob.brightspot;

import com.psddev.cms.db.Content;
import com.psddev.dari.db.*;
import com.psddev.dari.util.*;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.Taxon;

import java.io.IOException;
import java.util.*;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ToolUi.LabelFields("displayLong")
public class FanmobTopic extends Content {
    @Required
    @ToolUi.NoteHtml("The name of the topic on FanMob. Consult the <a href='https://dataclips.heroku.com/kcqvtivhrqoyulrfbdcmiyfrdytc'>list of topics</a> to see what's available.")
    @ToolUi.Placeholder("chicago-bulls")
    @Indexed(unique = true)
    private String name;

    @Required
    @ToolUi.Note("A human-friendly name for this topic.")
    @ToolUi.Placeholder("Chicago Bulls")
    private String displayLong;

    // The beforeSave() callback is called several times before the save occurs,
    // so use this as a kludge to prevent multiple requests checking for the topic.
    private transient boolean checkedTopic;

    public String getName() {
        return this.name;
    }

    @Override
    public void beforeSave() {
        State state = State.getInstance(this);
        if (!state.validate() || this.checkedTopic) {
            return;
        }

        String base = Settings.getOrDefault(String.class, "fanmob/apiBaseUrl", "https://www.fanmob.us/api");
        HttpGet getTopic = new HttpGet(base + "/topics/" + this.name);
        getTopic.setHeader("User-Agent", "FanMob-Brightspot/0.1.0");
        ResponseHandler<Boolean> responseHandler = new ResponseHandler<Boolean>() {
            public Boolean handleResponse(final HttpResponse response)
                throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();

                if (status >= 200 && status < 300) {
                    return true;
                } else if (status == 404) {
                    return false;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        try {
            HttpClient client = new DefaultHttpClient();
            Boolean topicExists = client.execute(getTopic, responseHandler);
            this.checkedTopic = true;
            if (!topicExists) {
                state.addError(state.getField("name"), "Does not exist on FanMob.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
