package us.fanmob.brightspot;

import com.psddev.cms.db.Content;
import com.psddev.dari.util.*;
import com.psddev.cms.db.ToolUi;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

@ToolUi.LabelFields("displayLong")
public class FanmobTopic extends Content implements FanMobObject {
    @Required
    @ToolUi.NoteHtml("The name of the topic on FanMob. Consult the <a href='https://dataclips.heroku.com/kcqvtivhrqoyulrfbdcmiyfrdytc'>list of topics</a> to see what's available.")
    @ToolUi.Placeholder("chicago-bulls")
    @Indexed(unique = true)
    private String name;

    @Required
    @ToolUi.Note("A human-friendly name for this topic.")
    @ToolUi.Placeholder("Chicago Bulls")
    private String displayLong;

    public String getName() {
        return this.name;
    }

    @Override
    public void create() {
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
            if (!topicExists) {
                this.as(FanMobObject.Status.class).setError("Does not exist of FanMob");
            }
        } catch (IOException e) {
            this.as(FanMobObject.Status.class).setError(e.getMessage());
        }
    }
}
