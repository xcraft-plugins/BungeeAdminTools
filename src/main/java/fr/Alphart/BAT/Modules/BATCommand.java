package fr.Alphart.BAT.Modules;

import static fr.Alphart.BAT.BAT.__;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.TabExecutor;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import fr.Alphart.BAT.BAT;

public abstract class BATCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor{
	private static final Pattern pattern = Pattern.compile("<.*?>");
	private final String name;
	private final String syntax;
	private final String description;
	private boolean runAsync = false;

	private int minArgs = 0;

	/**
	 * Constructor
	 * @param name  name of this command
	 * @param description description of this command
	 * @param permission  permission required to use this commands
	 * @param aliases  aliases of this commnad (optionnal)
	 */
	public BATCommand(final String name, final String syntax, final String description, final String permission, final String... aliases){
		super(name, permission, aliases);
		this.name = name;
		this.syntax = syntax;
		this.description = description;

		// Compute min args
		final Matcher matcher = pattern.matcher(syntax);
		while(matcher.find()){
			minArgs++;
		}

		final RunAsync asyncAnnot = getClass().getAnnotation(RunAsync.class);
		if(asyncAnnot != null){
			runAsync = true;
		}
	}

	public String getDescription(){
		return description;
	}

	public String getUsage(){
		final String usage = Joiner.on(' ').join(name, syntax, description);
		return usage;
	}

	/**
	 * Get a nice coloured usage
	 * @return coloured usage
	 */
	public String getFormatUsage(){
		return ChatColor.translateAlternateColorCodes('&', "&e" + name + " &6" + syntax + " &f-&B " + description);
	}

	@Override
	public void execute(final CommandSender sender, final String[] args) {
		try{
			Preconditions.checkArgument(args.length >= minArgs);
			if(runAsync){
				ProxyServer.getInstance().getScheduler().runAsync(BAT.getInstance(), new Runnable(){
					@Override
					public void run() {
						try{
							onCommand(sender, args);
						}catch(final IllegalArgumentException exception){
							if(exception.getMessage() == null){
								sender.sendMessage(__("&cAInvalid args. &BUsage : "));
								sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&e/") + getFormatUsage() ));
							} else {
								sender.sendMessage(__("&cInvalid args. &6" + exception.getMessage()));
							}
						}
					}			
				});
			}
			else{
				onCommand(sender, args);
			}
		}catch(final IllegalArgumentException exception){
			if(exception.getMessage() == null){
				sender.sendMessage(__("&cInvalid args. &BUsage : "));
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', "&e/") + getFormatUsage() ));
			} else {
				sender.sendMessage(__("&cInvalid args. &6" + exception.getMessage()));
			}
		}	
	}

	@Override
	public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
		final List<String> result = new ArrayList<String>();
		if(args.length == 0){
			sender.sendMessage( __("Add the first letter to autocomplete") );
			return result;
		}
		final String playerToCheck = args[args.length - 1];
		if ( playerToCheck.length() > 0 ){
			for ( final ProxiedPlayer player : ProxyServer.getInstance().getPlayers() ) {
				if(player.getName().substring(0, (playerToCheck.length()<player.getName().length())
				? playerToCheck.length() 
				: player.getName().length()).equalsIgnoreCase(playerToCheck)){
					result.add(player.getName());
				}
			}
		}
		return result;
	}
	
	public abstract void onCommand(final CommandSender sender, final String[] args) throws IllegalArgumentException;

	/**
	 * Use this annotation onCommand if the command need to be runned async
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RunAsync{} 

	/* Utils for command */
	/**
	 * Check if the sender is a player <br>
	 * Use for readibility
	 * @param sender
	 * @return true if the sender is a player otherwise false
	 */
	public boolean isPlayer(final CommandSender sender){
		if(sender instanceof ProxiedPlayer) {
			return true;
		}
		return false;
	}
}