package local.bwg.support;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * http://127.0.0.1:8080/requests/status.xml?command=pl_next&input=http//192.99.62.212:9968/listen.pls?sid=1
 * 127.0.0.1:8080/requests/status.xml?command=pl_next
 */
public class VLCSupport {
    public static String GetTrackName() {
        return getTextObj("now_playing");
    }
    public static String GetStationName() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "id-" +getTextObj("currentplid") + " " + getTextObj("title");
    }

    /**
     * go to track from id
     * @param id id number
     * @return boolean
     */
    public static boolean GoTo(String id){
        try {
            URL url = new URL("http://127.0.0.1:8080/requests/status.xml?command=pl_play&id=" + id);
            return vclControl(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Go to next track
     * @return boolean
     */
    public static boolean vlcNextTrack(){
        try {
            URL url = new URL ("http://127.0.0.1:8080/requests/status.xml?command=pl_next");
            return vclControl(url);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Go to previous track
     * @return boolean
     */
    public static boolean vlcPrevTrack(){
        try {
            URL url = new URL ("http://127.0.0.1:8080/requests/status.xml?command=pl_previous");
            return vclControl(url);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Sent vlc control commands
     * @param url command
     * @return boolean
     */
    private static boolean vclControl(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty  ("Authorization", "Basic " + DatatypeConverter.printBase64Binary(":1".getBytes()));
            InputStream content = (InputStream)connection.getInputStream();
            BufferedReader in   =  new BufferedReader (new InputStreamReader(content, StandardCharsets.UTF_8));
            in.close();
            content.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Deprecated
    public static String vlcOpen(String src){
        String[] par = src.split(" ");
        src = par[par.length-1];
        String srcBase = src.substring(5, src.length() - 6);
        src = "http://127.0.0.1:8080/requests/status.xml?command=in_play&input=" + srcBase;
        try {
            URL url = new URL (src);

            String encoding =  DatatypeConverter.printBase64Binary(":1".getBytes());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setRequestProperty  ("Authorization", "Basic " + encoding);
            InputStream content = (InputStream)connection.getInputStream();
            BufferedReader in   =  new BufferedReader (new InputStreamReader(content, "UTF-8"));
            in.close();
            content.close();
        } catch (IOException e) {
            return "#error#";
        }

        return srcBase;
    }

    static String getTextObj(String findObj){

        String name = "";
        try {
            URL url = new URL ("http://127.0.0.1:8080/requests/status.xml");

            String encoding =  DatatypeConverter.printBase64Binary(":1".getBytes());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty  ("Authorization", "Basic " + encoding);
            InputStream content = (InputStream) connection.getInputStream();
            BufferedReader in   =  new BufferedReader (new InputStreamReader(content, "UTF-8"));

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    if(line.indexOf(findObj) != 0)
                        name = line.substring(line.indexOf(findObj));
                } catch (Exception ignored){}
            }
            if (!Objects.equals(name, "")) {
                name = name.substring(name.indexOf(">") + 1, name.indexOf("<"));
            } else name = "unknown";

            System.out.println(name);

        } catch(Exception e) {
            name = "unknown";
            System.out.print("unknown");
        }
        return name;
    }
}
