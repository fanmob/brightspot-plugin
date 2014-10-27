# FanMob Brightspot Plugin

## Installation

Add the plugin as a dependency in `pom.xml`:

```xml
<!-- maven info goes here -->
```

Then add the following to `tomcat-context.xml`, filling in the API key you've been provided
for the `value`:

```xml
<Environment name="fanmob/apiKey" type="java.lang.String" value="YOUR_API_KEY_HERE" />
```

Restart Brighspot and create some Fanmob Topics from inside the CMS.  For now, Topics
must correspond to topics that already exist on Fanmob. Some topics you might
want to add include:

 Name                   | Display Name
------------------------|----------------------
 `washington-nationals` | Washington Nationals
 `washington-wizards`   | Washington Wizards
 `washington-capitals`  | Washington Capitals

For the full list of valid topics, see the link included on the Fanmob Topic creation form.

Once you've created some topics, you can create Fanmob Polls like any other
Content Type.  Fanmob Polls can be inserted into any rich text content using the
Enhancements feature.

## Support

For support, send email to support@fanmob.us

