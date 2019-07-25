package com.zunozap.impl;

import static com.zunozap.Log.out;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.border.EmptyBorder;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.BrowserContextParams;
import com.teamdev.jxbrowser.chromium.BrowserCore;
import com.teamdev.jxbrowser.chromium.BrowserException;
import com.teamdev.jxbrowser.chromium.BrowserPreferences;
import com.teamdev.jxbrowser.chromium.DownloadHandler;
import com.teamdev.jxbrowser.chromium.DownloadItem;
import com.teamdev.jxbrowser.chromium.PluginInfo;
import com.teamdev.jxbrowser.chromium.PopupContainer;
import com.teamdev.jxbrowser.chromium.PopupHandler;
import com.teamdev.jxbrowser.chromium.PopupParams;
import com.teamdev.jxbrowser.chromium.events.FinishLoadingEvent;
import com.teamdev.jxbrowser.chromium.internal.Environment;
import com.teamdev.jxbrowser.chromium.javafx.BrowserView;
import com.zunozap.Info;
import com.zunozap.LoadLis;
import com.zunozap.Settings;
import com.zunozap.Settings.Options;
import com.zunozap.UniversalEngine;
import com.zunozap.UniversalEngine.Engine;
import com.zunozap.ZFile;
import com.zunozap.ZFullScreenHandler;
import com.zunozap.ZunoAPI;
import com.zunozap.api.Plugin;
import com.zunozap.lang.Lang;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@Info(enableGC=false, engine = Engine.CHROME)
public class ZunoZapChrome extends ZunoAPI {

    private static JWindow l = new JWindow();
    private Random r = new Random();

    private final DownloadHandler dh = new DownloadHandler() {
        @Override public boolean allowDownload(DownloadItem i) { return !isUrlDownload(i.getDestinationFile().getName()); }
    };

    private final PopupHandler ph = new PopupHandler() {
        @Override public PopupContainer handlePopup(PopupParams pp) {
            return new PopupContainer() { @Override public void insertBrowser(Browser b, Rectangle r) {
                createTab(b.getURL());
                b.dispose();
            }};
        }
    };

    @Override
    public void init() throws IOException {
        setInstance(this);

        if (Environment.isMac()) BrowserCore.initialize();
        super.init();
    }

    public static void main(String[] args) throws IOException {
        JLabel z = new JLabel("ZUNOZAP");
        z.setFont(new Font("Dialog", Font.BOLD, 56));
        z.setForeground(Color.WHITE);
        z.setBorder(new EmptyBorder(15,30,15,30));
        l.setBackground(new Color(0,0,0,200));
        l.setContentPane(z);
        l.setVisible(true);
        l.pack();
        l.setLocationRelativeTo(null);

        launch(ZunoZapChrome.class, args);

        out("Shutting down BrowserCore");
        BrowserCore.shutdown();
        System.exit(0);
    }

    @Override
    public void start(Stage stage, Scene scene, StackPane root, BorderPane border) throws Exception {
        for (int i = 0; i < 10; i++) deleteDirs(new ZFile("engine" + i));

        Browser b = null;
        try {
            b = new Browser();
        } catch (BrowserException e) {
            ZFile f = new ZFile("engine" + r.nextInt(10));
            f.mk();
            f.deleteOnExit();
            b = new Browser(new BrowserContext(new BrowserContextParams(f.getAbsolutePath())));
        }

        createTab(Settings.tabPage);
        b.setUserAgent("ZunoZap/" + getInfo().version() + " " + b.getUserAgent());
        regMenuItems(menuFile, menuBook, aboutPageHTML(b.getUserAgent(), getJxPluginNames(b)), tb, Engine.CHROME);
        menuBar.getMenus().addAll(menuFile, menuBook);
        Settings.set(cssDir, scene);
        Settings.init(cssDir);
        Settings.changeStyle("ZunoZap default");
        Settings.save();
        scene.getStylesheets().add(ZunoAPI.stylesheet.toURI().toURL().toExternalForm());

        BrowserPreferences.setChromiumDir(data.getAbsolutePath());
        BrowserPreferences.setUserAgent(BrowserPreferences.getUserAgent() + " ZunoZap/" + getInfo().version());

        b.dispose(false);

        p.loadPlugins();
        hookonStart(stage, scene, tb);
        l.setVisible(false);
        l.dispose();
    }

    @Override
    public final void createTab(String url) {
        Browser b = null;

        try {
            b = new Browser();
        } catch (BrowserException e) {
            ZFile f = new ZFile("engine" + r.nextInt(10));
            f.mk();
            f.deleteOnExit();
            b = new Browser(new BrowserContext(new BrowserContextParams(f.getAbsolutePath())));
        }
        b.setUserAgent("ZunoZap/" + getInfo().version() + " " + b.getUserAgent());
        createTab(url, true, b, new BrowserView(b));
    }

    public final void createTab(String url, boolean load, Browser b, BrowserView web) {
        int tabnum = tb.getTabs().size() + 1;

        final Tab tab = new Tab(Lang.LOAD.tl);
        tab.setTooltip(new Tooltip("Tab " + tabnum));
        tab.setId("tab-"+tabnum);

        final Button back = new Button("<"), forward = new Button(">"), goBtn = new Button(Lang.GO.tl), bkmark = new Button("\u2606");
        Lang.a(() -> goBtn.setText(Lang.GO.tl));

        TextField field = new TextField("http://");
        HBox hBox = new HBox(back, forward, field, goBtn, bkmark);
        VBox vBox = new VBox(hBox, web);
        UniversalEngine e = new UniversalEngine(b);

        urlChangeLis(e, b, field, tab, bkmark);

        goBtn.setOnAction(v -> loadSite(field.getText(), e));
        field.setOnAction(v -> loadSite(field.getText(), e));

        back.setOnAction(v -> b.goBack());
        forward.setOnAction(v -> b.goForward());

        bkmark.setOnAction(v -> bookmarkAction(e, bmread, (t -> createTab(b.getURL())), bkmark, menuBook));

        String title = (b.getTitle() != null ? b.getTitle() : b.getURL());
        if (null == title && bmread.bm.containsKey(title)) bkmark.setText("\u2605");

        // Setting Styles
        field.setId("urlfield");
        field.setMaxWidth(400);
        hBox.setId("urlbar");
        HBox.setHgrow(field, Priority.ALWAYS);
        VBox.setVgrow(web, Priority.ALWAYS);
        vBox.autosize();

        b.getPreferences().setJavaScriptEnabled(Options.javascript.b);

        if (load)
            loadSite(url, e);

        b.setFullScreenHandler(new ZFullScreenHandler(stage));

        tab.setContent(vBox);
        tab.setOnCloseRequest(a -> onTabClosed(a.getSource()));

        if (allowPluginEvents()) for (Plugin pl : p.plugins) pl.onTabCreate(tab);

        final ObservableList<Tab> tabs = tb.getTabs();

        tabs.add(tabs.size() - 1, tab);
        tb.getSelectionModel().select(tab);
    }

    @Override
    protected void onTabClosed(Object s) {
        try {
            ((BrowserView) ((VBox) ((Tab) s).getContent()).getChildren().get(1)).getBrowser().dispose();
        } catch (Exception e) { ((BrowserView) ((Tab) s).getContent()).getBrowser().dispose(); }
    }

    public final void urlChangeLis(UniversalEngine u, final Browser b, final TextField urlField, final Tab tab, final Button bkmark) {
        b.setDownloadHandler(dh);
        b.setPopupHandler(ph);

        b.addLoadListener(new LoadLis(true) {
            @Override public void onFinishLoadingFrame(FinishLoadingEvent e) {
                String url = e.getBrowser().getURL();
                Platform.runLater(() -> {
                    tab.setText(b.getTitle());
                    if (bmread.bm.containsKey(b.getTitle() != null ? b.getTitle() : url)) 
                        bkmark.setText("\u2605");
                    changed(u, urlField, tab, urlField.getText(), url, bkmark, bmread);
                });
            }
        });
    }

    public static final String getJxPluginNames(Browser b) {
        ArrayList<String> names = new ArrayList<>();
        int size = b.getPluginManager().getPluginsInfo().size();
        for (PluginInfo i : b.getPluginManager().getPluginsInfo()) {
            String s = i.getName() + " " + i.getVersion();
            if (!names.contains(s)) names.add(s);
            else size--;
        }
        return (size != 0 ? "(" + size + "): " + names.toString().substring(1).replace("]", "") : "No Chromium plugins");
    }

}