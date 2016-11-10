package com.example.genkiplayer.server;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.contentdisplay.ContentActivity;
import com.example.genkiplayer.util.Utils;

import java.io.File;

public class MyServer extends fi.iki.elonen.NanoHTTPD {
    private final static int PORT = 8080;
    private ContentActivity activity;

    public MyServer(ContentActivity activity) {
        super(PORT);
        this.activity = activity;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String msg = "";
        if(uri.equals("/contentPing")) {
            MainConfig.getInstance().setNeedsReload();
            MainConfig.getInstance().resetLastUpdate();
            msg += "ok";
        } else if (uri.equals("/currentFilename")) {
            for(File file : activity.regularContentPath().listFiles()) {
                msg += "Regular Content Path " + file.getName() + " " + file.length() + " " + Utils.formatDate(file.lastModified());
                msg += "<br />";
            }
            for(File file : activity.regularContentTempPath().listFiles()) {
                msg += "Temp Content Path " + file.getName() + " " + file.length() + " " + Utils.formatDate(file.lastModified());
                msg += "<br />";
            }
        } else {
            msg += "<html><body><h1>Hello server</h1>\n";
            msg += "<p>We serve " + session.getUri() + " !</p>";
            msg = msg + "</body></html>\n";
        }
        return newFixedLengthResponse(msg);

    }
    // TODO serve thumbnails

}
