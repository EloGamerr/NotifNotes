package fr.lorek.notifnotes;

import jdk.tools.jmod.Main;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class MainNotif {

    private static String getExecutionFromContent(String result) throws IOException {
        String[] resultSplit = result.split("name=\"execution\"");
        if (resultSplit.length >= 2) {
            String executionLine = resultSplit[1];
            executionLine = executionLine.split(">")[0];
            executionLine = executionLine.trim();
            String[] executionLineSplit = executionLine.split("value=\"");

            if (executionLineSplit.length >= 2) {
                executionLine = executionLineSplit[1];
                executionLine = executionLine.replaceAll("\".*", "");


                return executionLine;
            }
        }

        return null;
    }

    public static void main(String[] args) {
        PropertiesManager propertiesManager = new PropertiesManager();

        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();

            while(true) {
                System.out.println("Entrée de boucle");

                HttpGet httpget = new HttpGet("https://cas.univ-lyon1.fr/cas/login?service=" + propertiesManager.getUrlTomuss() + "/?unsafe=1");

                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    String result = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                    entity.getContent().close();
                    String execution = getExecutionFromContent(result);
                    if (execution != null) {
                        HttpPost httpost = new HttpPost("https://cas.univ-lyon1.fr/cas/login?service=" + propertiesManager.getUrlTomuss() + "/?unsafe=1");

                        List<NameValuePair> nvps = new ArrayList<>();
                        nvps.add(new BasicNameValuePair("username", propertiesManager.getLogin()));
                        nvps.add(new BasicNameValuePair("password", propertiesManager.getPassword()));
                        nvps.add(new BasicNameValuePair("lt", ""));
                        nvps.add(new BasicNameValuePair("execution", execution));
                        nvps.add(new BasicNameValuePair("_eventId", "submit"));
                        nvps.add(new BasicNameValuePair("submit", "SE CONNECTER"));

                        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

                        response = httpclient.execute(httpost);
                        entity = response.getEntity();

                        if (entity != null) {
                            entity.getContent().close();
                        }

                        if (response.getFirstHeader("Location") == null) {
                            displayTray("Identifiants incorrects");
                            return;
                        }

                        httpget = new HttpGet(response.getFirstHeader("Location").getValue());

                        response = httpclient.execute(httpget);
                        entity = response.getEntity();

                        if (entity != null) {
                            result = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                            entity.getContent().close();
                        }
                    }

                    String[] resultSplit = result.split("display_update\\(");

                    if (resultSplit.length >= 2) {
                        result = resultSplit[1];
                        resultSplit = result.split("\n");
                        result = resultSplit[0];
                        while (!result.isEmpty() && result.toCharArray()[result.length()-1] != ']') {
                            result = result.substring(0, result.length()-1);
                        }

                        result = result.replaceAll("\\\\", "\\\\\\\\");
                        result = result.replaceAll("\\\\\\\\\"", "\\\\\\\\\\\\\"");
                        JSONArray jsonArray = new JSONArray(result);
                        Iterator it = jsonArray.iterator();
                        while (it.hasNext()) {
                            Object o = it.next();
                            if (o instanceof JSONArray) {
                                JSONArray jsonArray1 = (JSONArray) o;
                                if (jsonArray1.getString(0).equals("Grades")) {
                                    String grades = jsonArray1.getJSONArray(1).getJSONArray(0).toString();

                                    System.out.println("Vérification des notes");

                                    try {
                                        FileReader fileReader = new FileReader("grades.json");
                                        BufferedReader reader = new BufferedReader(fileReader);

                                        List<String> gradesList = getNewGrades(new JSONArray(reader.readLine()), jsonArray1.getJSONArray(1).getJSONArray(0));
                                        if (!gradesList.isEmpty())
                                            displayTray(gradesList.get(0));

                                        fileReader.close();
                                    } catch (FileNotFoundException ex) {

                                    }

                                    FileWriter fileWriter = new FileWriter("grades.json");
                                    PrintWriter printWriter = new PrintWriter(fileWriter);
                                    printWriter.print(grades);
                                    printWriter.close();

                                    System.out.println("Fin de vérification des notes");
                                }
                            }
                        }
                    }
                }

                Thread.sleep(30000);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        } catch (AWTException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.err.println("Une erreur a eu lieu ! Attendez 15 secondes entre chaque utilisation !");
        //displayTray("Une erreur a eu lieu ! Attendez 15 secondes entre chaque utilisation !");
    }

    private static List<String> getNewGrades(JSONArray oldGrades, JSONArray newGrades) {
        List<String> grades = new ArrayList<>();
        for (int i = 0; i < newGrades.length() ; ++i) {
            JSONObject newJsonObject = newGrades.getJSONObject(i);
            for (int j = 0; j < oldGrades.length(); ++j) {
                JSONObject oldJsonObject = oldGrades.getJSONObject(j);
                if (newJsonObject.getInt("code") == oldJsonObject.getInt("code")) {
                    if (!newJsonObject.toString().equalsIgnoreCase(oldJsonObject.toString())) {
                        JSONArray newJsonArray = newJsonObject.getJSONArray("columns");
                        JSONArray oldJsonArray = oldJsonObject.getJSONArray("columns");
                        for (int k = 0; k < newJsonArray.length(); ++k) {
                            JSONObject newJsonObject1 = newJsonArray.getJSONObject(k);
                            if (newJsonObject1.getString("type").equals("Note") || newJsonObject1.getString("type").equals("Moy")) {
                                boolean oldContainsNote = false;
                                for (int l = 0; l < oldJsonArray.length(); ++l) {
                                    JSONObject oldJsonObject1 = oldJsonArray.getJSONObject(l);
                                    if (newJsonObject1.getString("the_id").equals(oldJsonObject1.getString("the_id"))) {
                                        oldContainsNote = true;
                                    }
                                }
                                if (!oldContainsNote) {
                                    int grade = newJsonObject.getJSONArray("line").getJSONArray(k).getInt(0);
                                    grades.add(newJsonObject.getString("table_title").replaceAll("\\\\u00e9", "e") + " " + newJsonObject1.getString("title") + " " + grade);
                                }
                            }
                        }
                    }
                }
            }
        }
        return grades;
    }

    private static void displayTray(String text) throws AWTException {
        if (SystemTray.isSupported()) {
            //Obtain only one instance of the SystemTray object
            SystemTray tray = SystemTray.getSystemTray();

            //Alternative (if the icon is on the classpath):
            Image image = Toolkit.getDefaultToolkit().createImage(MainNotif.class.getResource("logo_animated.gif"));

            TrayIcon trayIcon = new TrayIcon(image, "DorianSquare");
            //Let the system resize the image if needed
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            trayIcon.displayMessage("NotifNotes", text, TrayIcon.MessageType.INFO);
        } else {
            System.out.println(text);
        }
    }
}
