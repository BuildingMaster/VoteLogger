package main.java.net.bigbadcraft.votelogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteLogger extends JavaPlugin implements Listener {
	
	private final HashMap<String, Integer> votes = new HashMap<String, Integer>();
	private final ValueComparator comparator = new ValueComparator(votes);
	private final TreeMap<String, Integer> sortedVotes = new TreeMap<String, Integer>(comparator);
	
	private final ChatColor T = ChatColor.DARK_AQUA;
	private final ChatColor A = ChatColor.AQUA;
	private final String PREFIX = A + "[VoteLogger]";
	
	private File votesFile;
	private FileConfiguration votesConf;
	
	private boolean enableLogging;
	private int topVotes;
	
	public void onEnable() {
		
		saveDefaultConfig();
		
		enableLogging = getConfig().getBoolean("enableLogging");
		topVotes = getConfig().getInt("topVotes");
		
		votesFile = new File(getDataFolder(), "votes" + getCurrentMonth().substring(0, 3) + ".yml");
		
		/* Create votes file */
		if (!votesFile.exists()) {
			try {
				votesFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		votesConf = YamlConfiguration.loadConfiguration(votesFile);
		reloadVotesConf();
		
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("votelogger").setExecutor(this);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		
		/* Just to register their name and votes to 0 */
		String name = event.getPlayer().getName();
		
		if (!votes.containsKey(name)) {
			reloadVotesConf();
			votesConf.set(name, 0);
			saveVotesConf();
			log(Level.INFO, "Registered " + name + " to file.");
		} 
		
	}
	
	@EventHandler
	public void onVote(VotifierEvent event) {
		
		String name = event.getVote().getUsername();
		
		if (!votesConf.contains(name)) {
			reloadVotesConf();
			votesConf.set(name, 1);
			saveVotesConf();
			log(Level.INFO, "Saved " + name + " votes to file.");
		} else {
			reloadVotesConf();
			votesConf.set(name, votesConf.getInt(name) + 1);
			saveVotesConf();
			log(Level.INFO, "Saved " + name + " votes to file.");
		}
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmdObj, String lbl, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (cmdObj.getName().equalsIgnoreCase("votelogger")) {
				if (args.length == 0) {
					player.sendMessage(helpMenu());
				}
				else if (args.length == 1) {
					if (args[0].equalsIgnoreCase("top")) {
						displayTopVotes(player);
					}
					else if (args[0].equalsIgnoreCase("view")) {
						player.sendMessage(PREFIX + T + " Your votes for " + getCurrentMonth() + " is: " + votesConf.getInt(player.getName()));
					}
				}
				else if (args.length == 2) {
					if (args[0].equalsIgnoreCase("view")) {
						String name = args[1];
						
						if (votesConf.contains(name)) {
							player.sendMessage(PREFIX + T + " " + name + "'s votes for " + getCurrentMonth() + " is: " + votesConf.getInt(name));
						} else {
							player.sendMessage(PREFIX + T + " " + name + " has not votes.");
						}
						
					}
				}
			}
		}
		
		return true;
	}
	
	private void reloadVotesConf() {
		if (votesFile == null) {
			votesFile = new File(getDataFolder(), "votes" + getCurrentMonth().substring(0, 3) + ".yml");
		}
		
		votesConf = YamlConfiguration.loadConfiguration(votesFile);
		
		InputStream stream = getResource("votes" + getCurrentMonth().substring(0, 3) + ".yml");
		if (stream != null) {
			YamlConfiguration streamConf = YamlConfiguration.loadConfiguration(stream);
			votesConf.setDefaults(streamConf);
		}
	}
	
	private void saveVotesConf() {
		if (votesConf == null || votesFile == null) {
			return;
		}
		
		try {
			votesConf.save(votesFile);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Could not save credits config to " + votesFile, e);
		}
	}
	
	private String getCurrentMonth() {
        switch(Calendar.getInstance().get(Calendar.MONTH) + 1) {
        case 1: return "January"; 
        case 2: return "February";
        case 3: return "March";
        case 4: return "April";
        case 5: return "May";
        case 6: return "June";
        case 7: return "July";
        case 8: return "August";
        case 9: return "September";
        case 10: return "October";
        case 11: return "November";
        case 12: return "December";
        default: return "";
        }
	}
	
	private String helpMenu() {
		return T + "---------------(" + A + "VoteLogger" + T + ")---------------\n"
				+ A + "Shortcut:" + T + " /vl or /vlogger\n"
				+ A + "-/votelogger top" + T + " - Display top " + topVotes + " votes.\n"
				+ A + "-/votelogger view" + T + " - Display your votes.\n"
				+ A + "-/votelogger view <player>" + T + " - Display player's votes\n"
				+ T + "-----------------------------------------";
	}
	
	private void log(Level level, String message) {
		if (enableLogging)
			Bukkit.getLogger().log(level, message);
	}
	
	private void displayTopVotes(final Player player) {

		for (String pathName : votesConf.getKeys(true)) {
			if (!votes.containsKey(pathName)) {
				votes.put(pathName, votesConf.getInt(pathName));
			}
		}
		
		sortedVotes.putAll(votes);
		
		if (sortedVotes.size() < topVotes) {
			player.sendMessage(PREFIX + T + " Unable to display. Total votes for " + getCurrentMonth() + " is less than " + topVotes + ".");
			return;
		}
	
		reloadVotesConf();
		
		player.sendMessage(PREFIX + T + " Ordering top votes for " + getCurrentMonth() + ".");
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
			int position = 1;
			@Override
			public void run() {
				player.sendMessage(T + "----------(" + A + "Top Votes" + T + ")----------");
				for (Entry<String, Integer> entry : sortedVotes.entrySet()) {
					
					if (position != topVotes + 1) 
						player.sendMessage(A + "" + position++ + ". " + entry.getKey() + ": " + T + entry.getValue());
					
				}
				player.sendMessage(T + "------------------------------");
			}
		}, 20 * 5);
	}
}

class ValueComparator implements Comparator<String> {

	Map<String, Integer> base;
	public ValueComparator(Map<String, Integer> base) {
		this.base = base;
	}
	
	@Override
	public int compare(String a, String b) {
		if (base.get(a) >= base.get(b)) {
			return -1;
		}
		return 1;
	}
	
}
