module net.sf.linuxorg.pcal.pcalendar {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.prefs;


    opens net.sf.linuxorg.pcal.pcalendar to javafx.fxml;
    exports net.sf.linuxorg.pcal;
}