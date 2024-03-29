package local.bwg;

import local.bwg.model.TeamspeakUser;
import local.bwg.support.FileReaderWriterExp;
import local.bwg.support.SaveSupport;
import local.bwg.support.VLCSupport;
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import local.bwg.telegram.AppTelegramInline;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

import static com.github.theholywaffle.teamspeak3.api.ChannelProperty.CHANNEL_NAME;

/**
 * Core 2.5
 */

public class TeamspeakCoreExp {
    private static volatile int clientId;

    private static ArrayList<TeamspeakUser> userdatabase = new ArrayList<>();

    private static ArrayList<TeamspeakUser> lastuserdatabase = new ArrayList<>();

    private static SaveSupport saveSupport;

    private static AppTelegramInline appTelegramInline = new AppTelegramInline();

    private TS3Query ts3Query = null;

    TeamspeakCoreExp(String address, final String port, final String login, final String password) {

        System.out.println(address + " | " + port + " | " + login + " | " + password);

        appTelegramInline.run();

        saveSupport = new FileReaderWriterExp();

        final TS3Config cfg = new TS3Config();
        cfg.setHost(address);
        cfg.setDebugLevel(Level.ALL);
        cfg.setReconnectStrategy(ReconnectStrategy.constantBackoff());
        cfg.setConnectionHandler(new ConnectionHandler() {
            @Override
            public void onConnect(TS3Query ts3Query) {
                stuffThatNeedsToRunEveryTimeTheQueryConnects(ts3Query.getApi(), login, password, port);
            }

            @Override
            public void onDisconnect(TS3Query ts3Query) {
                // Nothing
            }
        });

        ts3Query = new TS3Query(cfg);
        ts3Query.connect();

        stuffThatOnlyEverNeedsToBeRunOnce(ts3Query.getApi());
    }

    private static String printLastUsers() {
        StringBuilder lusers = new StringBuilder();
        for(TeamspeakUser u : lastuserdatabase) {
            lusers.append("\n").append(u.getTime()).append(": ").append(u.getuName());
        }
        return lusers.toString();
    }
    private static void addLastUser(TeamspeakUser user) {
        if (lastuserdatabase.size() > 15) {
            lastuserdatabase.remove(lastuserdatabase.size()-1);
        }
        lastuserdatabase.add(0, user);
    }

    private static void stuffThatNeedsToRunEveryTimeTheQueryConnects(final TS3Api api, String login, final String password, String port) {
        //api.selectVirtualServerByPort(Integer.valueOf(port));
        api.selectVirtualServerById(1);
        api.login(login, password);

        clientId = api.whoAmI().getId();
        api.moveClient(clientId, 32);

        api.registerAllEvents();
        api.setNickname("UnicBot");

        saveLog(api.whoAmI().getNickname() + " join server");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Date date = new Date();
                    SimpleDateFormat formatForDateNow = new SimpleDateFormat("HH:mm");
                    String formatTime = formatForDateNow.format(date);

                    Map<ChannelProperty, String> options_c1 = new HashMap<>();
                    options_c1.put(CHANNEL_NAME, "[spacer.time]   TIME : " + formatTime + "  UTC+9");
                    api.editChannel(22, options_c1);

                    for (TeamspeakUser u : userdatabase) {
                        if (u.getWakeUp().equals(formatTime)) {
                            api.pokeClient(u.isuID(), "Wake Up!");
                            u.setWakeUp("");
                        }
                    }

                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
        new Thread(() -> {
            String track = "known";
            String station = "known";
            while (true){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String temp = "Station: " + VLCSupport.GetStationName().replaceAll(convertToUTF8("[^0-9A-Za-zа-яА-Я_ -]"), "");
                temp = (temp.length() > 40) ? temp.substring(0, 39) : temp;
                if (!temp.equals(station)){
                    Map<ChannelProperty, String> options_c1 = new HashMap<>();
                    options_c1.put(CHANNEL_NAME, temp);
                    api.editChannel(33, options_c1);
                    if (!temp.equals("Station:"))
                        saveLog("radiouniclog", VLCSupport.GetStationName());
                    station = temp;
                }
                temp = "Track: " + VLCSupport.GetTrackName().replaceAll(convertToUTF8("[^0-9A-Za-zа-яА-Я_ -]"), "").replaceAll("amp", "");
                temp = (temp.length() > 40) ? temp.substring(0, 39) : temp;
                if (!temp.equals(track)){
                    Map<ChannelProperty, String> options_c1 = new HashMap<>();
                    options_c1.put(CHANNEL_NAME,  temp);
                    api.editChannel(31, options_c1);
                    if (!temp.equals("Track:  -"))
                        saveLog("radiouniclog", VLCSupport.GetTrackName());
                    track = temp;
                }
            }
        }).start();
    }
    private static String convertToUTF8(String text) {
        return new String(text.getBytes(), StandardCharsets.UTF_8);
    }
    private static void buckGroundTasks(final TS3Api api) {
        for (Client c : api.getClients()) {

            TeamspeakUser user = (TeamspeakUser) saveSupport.loadJson(c.getUniqueIdentifier());
            if (user == null) {
                user = new TeamspeakUser(c.getNickname(), c.getId(), c.getUniqueIdentifier());
                saveSupport.saveJson(user);
            }

            user.updateTime();
            user.setuName(c.getNickname());
            user.setuID(c.getId());

            userdatabase.add(user);
        }
    }

    private void stuffThatOnlyEverNeedsToBeRunOnce(final TS3Api api) {

        buckGroundTasks(api);

        api.addTS3Listeners(new TS3EventAdapter() {
            @Override
            public void onClientJoin(ClientJoinEvent e) {
                api.sendPrivateMessage(e.getClientId(),
                        " welcome to the party! " +
                                "\n Commands: " +
                                "\n !use loginnotify" +
                                "\n !mytime" +
                                "\n !wakeup");
                //log
                saveLog(e.getClientNickname() + " joined server");

                //telegram
                appTelegramInline.sendMessage(e.getClientNickname() + " joined server");

                try {
                    //load user data from file
                    TeamspeakUser user = (TeamspeakUser) saveSupport.loadJson(e.getUniqueClientIdentifier());
                    if (user == null) {
                        user = new TeamspeakUser(e.getClientNickname(), e.getClientId(), e.getUniqueClientIdentifier());
                        saveSupport.saveJson(user);
                    }

                    user.updateTime();
                    user.setuName(e.getClientNickname());
                    user.setuID(e.getClientId());

                    api.sendPrivateMessage(e.getClientId(), "You total time: " + user.getTotalTimeString());

                    for (TeamspeakUser u : userdatabase) {
                        if(u.getuPrivilegeLevel() == 27) {
                            api.sendPrivateMessage(u.isuID(), "ui: " + e.getUniqueClientIdentifier());
                        }
                        if(u.isLoginNotifyStatus()) {
                            api.sendPrivateMessage(u.isuID(),user.getuName() + " join server.");
                        }
                        if(u.isuID() == e.getClientId()) {
                            user = null;
                            break;
                        }
                    }
                    if (user != null) {
                        userdatabase.add(user);
                    }
                } catch (Exception ignore) {
                    System.out.println("Error: uID");
                }
            }
            @Override
            public void onClientLeave(ClientLeaveEvent e) {
                try {
                    TeamspeakUser user = null;
                    for (TeamspeakUser u : userdatabase) {
                        if(e.getClientId() == u.isuID()){
                            user = u;
                            break;
                        }
                    }


                    if(user != null) {
                        saveLog(user.getuName() + " left server");
                        addLastUser(user);

                        //telegram
                        appTelegramInline.sendMessage(user.getuName() + " left server");
                    }


                    for (TeamspeakUser u : userdatabase) {
                        if(u.isLoginNotifyStatus()) {
                            if(user != null)
                                api.sendPrivateMessage(u.isuID(),user.getuName() + " left server.");
                        }
                    }

                    if(user != null) {
                        // update user properties
                        user.updateTotalTime();

                        userdatabase.remove(user);
                        saveSupport.saveJson(user);
                    }
                } catch (Exception ignore) {
                    System.out.println("Error: uID not found");
                }
            }
            @Override
            public void onClientMoved(ClientMovedEvent e) {
                if (e.getTargetChannelId() == 32) {
                    int movingClientId = e.getClientId();
                    String name = api.getClientInfo(movingClientId).getNickname();

                    String stationname = convertToUTF8(VLCSupport.GetStationName());
                    String trackname = convertToUTF8(VLCSupport.GetTrackName());
                    byte[] msg = ("\n Hi, " + name + "\n Station: " + stationname
                            + "\n Track: " + trackname).getBytes();
                    api.sendChannelMessage(new String(msg, StandardCharsets.UTF_8));
                    api.sendChannelMessage(
                                    "\n Commands: " +
                                    "\n !use loginnotify или !ln - включить/выключить уведомления" +
                                    "\n !track" +
                                    "\n !station" +
                                    "\n !next" +
                                    "\n !prev");
                }
            }

            @Override
            public void onTextMessage(TextMessageEvent e) {
                // Only react to channel messages not sent by the query itself
                if (e.getTargetMode() == TextMessageTargetMode.CHANNEL
                        && e.getInvokerId() != clientId
                        && api.getClientInfo(e.getInvokerId()).getChannelId() == 32) {
                    String message = e.getMessage().toLowerCase();
                    /* Music control
                     * vlc control *
                    */
                    if (message.equals("!track")) {
                        //api.sendPrivateMessage(e.getInvokerId(), VLCSupport.GetTrackName());
                        api.sendChannelMessage(VLCSupport.GetTrackName());
                    } else if (message.equals("!station")) {
                        //api.sendPrivateMessage(e.getInvokerId(), VLCSupport.GetTrackName());
                        api.sendChannelMessage(VLCSupport.GetStationName());
                    } else if(message.startsWith("!next")){
                        if (VLCSupport.vlcNextTrack()) {
                            api.sendChannelMessage("Station: " + VLCSupport.GetStationName());
                        } else {
                            api.sendChannelMessage("Failed!");
                        }
                    } else if(message.startsWith("!prev")){
                        if (VLCSupport.vlcPrevTrack()) {
                            api.sendChannelMessage("Station: " + VLCSupport.GetStationName());
                        } else {
                            api.sendChannelMessage("Failed!");
                        }
                    } else if(message.startsWith("!goto")){
                        try {
                            String id = message.split(" ")[1];
                            if (VLCSupport.GoTo(id)) {
                                api.sendChannelMessage("Station: " + VLCSupport.GetStationName());
                            } else {
                                api.sendChannelMessage("Failed!");
                            }
                        } catch (Exception ignore) {
                            api.sendChannelMessage(" !goto 109");
                        }
                    }
                }
                if (e.getTargetMode() == TextMessageTargetMode.CLIENT && e.getInvokerId() != clientId) {
                    String message = e.getMessage().toLowerCase();
                    if (message.startsWith("!last")) {
                        api.sendPrivateMessage(e.getInvokerId(), printLastUsers());
                    } else if (message.startsWith("!use loginnotify") || message.startsWith("!ul")) {
                        for (TeamspeakUser u : userdatabase) {
                            if (e.getInvokerId() == u.isuID()) {
                                if (u.isLoginNotifyStatus()) {
                                    u.setLoginNotifyStatus(false);
                                    api.sendPrivateMessage(e.getInvokerId(), "Login notification is off for u.");
                                } else {
                                    u.setLoginNotifyStatus(true);
                                    api.sendPrivateMessage(e.getInvokerId(), "Login notification is on for u.");
                                }
                                break;
                            }
                        }
                    } else if (message.startsWith("!wakeup")){
                        for (TeamspeakUser u : userdatabase) {
                            if (e.getInvokerId() == u.isuID()) {
                                try {
                                    u.setWakeUp(message.substring(8));
                                    api.sendPrivateMessage(e.getInvokerId(), "wakeup set " + message.substring(8));
                                } catch (Exception ignore) {
                                    api.sendPrivateMessage(e.getInvokerId(), "use: !wakeup [time]");
                                    break;
                                }
                                break;
                            }
                        }
                    } else if (message.startsWith("!cmd")) {
                        //WinAPISupport winApi = new WinAPISupport();
                        //winApi.exCMD(message.substring(4));
                    } else if (message.startsWith("!shutdown 0")) {
                        api.sendPrivateMessage(e.getInvokerId(), ": Bot Close "  );
                        closeBot(e.getInvokerUniqueId());
                    } else if (message.startsWith("!use")) {
                        //api.sendPrivateMessage(e.getInvokerId(), ": !use notify " );
                        api.sendPrivateMessage(e.getInvokerId(), ": !use loginnotify "  );
                    } else if (message.startsWith("!mytime")) {
                        //api.sendPrivateMessage(e.getInvokerId(), ": !use notify " );
                        for (TeamspeakUser u : userdatabase) {
                            if (e.getInvokerId() == u.isuID()) {
                                api.sendPrivateMessage(e.getInvokerId(), "Total time : " + u.getTotalTimeString());
                            }
                        }
                    }
                    else if (message.startsWith("!get")) {
                        try {
                            String[] cmd = message.split(" ");
                            switch (cmd[1]) {
                                case "all": {
                                    for (String filename : saveSupport.getAllFilesName(null)) {
                                        TeamspeakUser user = (TeamspeakUser) saveSupport.loadJson(filename);
                                        if (user == null) continue;
                                        String lastLoginDate = saveSupport.getFileLastModified(filename);
                                        api.sendPrivateMessage(e.getInvokerId(),
                                                "\n Name: " + user.getuName()
                                                        + "\n Identifier: " + user.getuUnicID()
                                                        + "\n TotalTime: " + user.getTotalTimeStringNoCalc()
                                                        + "\n LastLogin: " + lastLoginDate
                                        );
                                    }
                                    break;
                                }
                                case "users": {
                                    for (TeamspeakUser u : userdatabase) {
                                        api.sendPrivateMessage(e.getInvokerId(),
                                                "\n Name: " + u.getuName()
                                                        + "\n Identifier: " + u.getuUnicID()
                                        );
                                    }
                                    break;
                                }
                                case "user": {
                                    TeamspeakUser user = (TeamspeakUser) saveSupport.loadJson(cmd[2]);
                                    String lastlogindate = saveSupport.getFileLastModified(cmd[2]);
                                    api.sendPrivateMessage(e.getInvokerId(),
                                            "\n Name: " + user.getuName()
                                                    + "\n Identifier: " + user.getuUnicID()
                                                    + "\n TotalTime: " + user.getTotalTimeStringNoCalc()
                                                    + "\n LastLogin: " + lastlogindate
                                    );
                                    break;
                                }
                            }
                        } catch (Exception ignore){
                            api.sendPrivateMessage(e.getInvokerId(),"incorrect syntax");
                        }
                    }
                }
            }
        });
    }

    private static void saveLog(String data){
        saveSupport.saveLog("unicbotlog", data);
    }

    private static void saveLog(String filename, String data){
        saveSupport.saveLog(filename, data);
    }

    private void closeBot(String uID) {
        for (TeamspeakUser u : userdatabase) {
            u.updateTotalTime();
            saveSupport.saveJson(u);
        }
        saveLog(uID + ": shutdown 0");
        ts3Query.exit();
        System.exit(0);
    }


}
