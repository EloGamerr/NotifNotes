package fr.lorek.notifnotes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesManager {
    private final Properties properties;

    public PropertiesManager() {
        this.properties = new Properties();
        File file = new File("settings.txt");
        try {
            if (!file.exists()) {
                this.initProperties();
            }

            this.properties.load(new FileInputStream("settings.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLogin() {
        return this.properties.getProperty("login", "pXXXXXXX");
    }

    public String getPassword() {
        return this.properties.getProperty("password", "pass");
    }

    public String getUrlTomuss() {
        return this.properties.getProperty("urlTomuss", "https://tomuss.univ-lyon1.fr/S/2021/Automne");
    }

    public void initProperties() {
        this.properties.setProperty("login", "pXXXXXXX");
        this.properties.setProperty("password", "pass");
        this.properties.setProperty("urlTomuss", "https://tomuss.univ-lyon1.fr/S/2021/Automne");
        save();
    }

    private void save() {
        try {
            this.properties.store(new FileOutputStream("settings.txt"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
