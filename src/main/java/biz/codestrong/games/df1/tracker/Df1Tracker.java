package biz.codestrong.games.df1.tracker;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public class Df1Tracker {

	/**
	 * How often to check the lobby for players (in seconds)
	 */
	private final static int CHECK_INTERVAL = 60;

	/**
	 * The lobby URL
	 */
	private final static String URL = "http://df1stats.com/lobby.php";

	/**
	 * How many players were online when checked last time
	 */
	private Integer onlineLastCount = 0;

	/**
	 * Text displayed to the user
	 */
	private final static String MESSAGE_NO_PLAYERS = "No players online";
	private final static String MESSAGE_ONE_PLAYERS = "1 player online";
	private final static String MESSAGE_MANY_PLAYERS = "{X} players online";
	private final static String MESSAGE_TOOLTIP_ONE = "DF1 Tracker (1 player)";
	private final static String MESSAGE_TOOLTIP_MANY = "DF1 Tracker ({X} players)";

	public static void main(String[] args) {
		// check if system tray is supported
		if (!SystemTray.isSupported()) {
			System.out.println("System tray is not supported. Please report this error to support@codestrong.biz");
			return;
		}

		try {
			Df1Tracker tracker = new Df1Tracker();
			tracker.createAndAddApplicationToSystemTray();
			tracker.startProcess();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		startProcess();
	}

	private void createAndAddApplicationToSystemTray() throws IOException {
		// get the systemTray of the system
		SystemTray systemTray = SystemTray.getSystemTray();

		// get image
		// Image image =
		// Toolkit.getDefaultToolkit().getImage("src/main/resources/df1.jpg");
		Image image = Toolkit.getDefaultToolkit().getImage(Df1Tracker.class.getResource("/df1.jpg"));

		// popup menu
		PopupMenu trayPopupMenu = new PopupMenu();

		// popup menu: exit
		MenuItem close = new MenuItem("Exit");
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		trayPopupMenu.add(close);

		// setting tray icon
		TrayIcon trayIcon = new TrayIcon(image, MESSAGE_TOOLTIP_MANY.replace("{X}", "0"), trayPopupMenu);

		// adjust to default size as per system recommendation
		trayIcon.setImageAutoSize(true);

		try {
			systemTray.add(trayIcon);
		} catch (AWTException awtException) {
			awtException.printStackTrace();
		}

		Map<String, Integer> stats = null;
		String message = null;

		while (true) {
			// visit the lobby and get number of players on each server
			stats = scrape();

			// determine if players have joined/left a server
			message = analyseStats(stats);

			if (message != null) {
				trayIcon.displayMessage("Delta Force 1", message, TrayIcon.MessageType.INFO);
			}

			String tooltip = "";

			if (onlineLastCount == 1) {
				tooltip = MESSAGE_TOOLTIP_ONE;
			} else {
				tooltip = MESSAGE_TOOLTIP_MANY.replace("{X}", onlineLastCount.toString());
			}

			trayIcon.setToolTip(tooltip);

			sleep(CHECK_INTERVAL);
		}
	}

	private static void startProcess() {
		Thread thread = new Thread(new Runnable() {
			public void run() {

			}
		});

		thread.start();
	}

	private String analyseStats(Map<String, Integer> stats) {
		String message = null;

		Integer currentCount = 0;

		for (Map.Entry<String, Integer> entry : stats.entrySet()) {
			currentCount += entry.getValue();
		}

		if (currentCount - onlineLastCount != 0) {
			if (currentCount == 0) {
				message = MESSAGE_NO_PLAYERS;
			} else if (currentCount == 1) {
				message = MESSAGE_ONE_PLAYERS;
			} else {
				message = MESSAGE_MANY_PLAYERS.replace("{X}", currentCount.toString());
			}
		}

		// remember results of this scrape
		onlineLastCount = currentCount;

		return message;
	}

	private void sleep(int sec) {
		try {
			Thread.sleep(sec * 1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private Map<String, Integer> scrape() {
		Document document;
		try {
			document = Jsoup.connect(URL).get();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		Elements elements = document.select("body center table tr");

		Map<String, Integer> stats = new HashMap<String, Integer>();

		for (int i = 1; i < elements.size(); i++) {
			Element row = elements.get(i);

			String serverName = row.child(0).text();
			String playerCount = row.child(3).text();

			String online = playerCount.split("/")[0];
			online = cleanText(online);

			Integer players = Integer.parseInt(online);

			stats.put(serverName, players);
		}

		return stats;
	}

	private String cleanText(String dirty) {
		String clean = dirty.trim();
		clean = Jsoup.clean(clean, Whitelist.none());
		clean = clean.replace("&nbsp;", "");
		return clean;
	}

}