package com.flansmod.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import com.flansmod.common.driveables.ItemPlane;
import com.flansmod.common.driveables.ItemVehicle;
import com.flansmod.common.driveables.PlaneType;
import com.flansmod.common.driveables.VehicleType;
import com.flansmod.common.driveables.mechas.ItemMecha;
import com.flansmod.common.driveables.mechas.ItemMechaAddon;
import com.flansmod.common.driveables.mechas.MechaItemType;
import com.flansmod.common.driveables.mechas.MechaType;
import com.flansmod.common.enchantments.GloveType;
import com.flansmod.common.enchantments.ItemGlove;
import com.flansmod.common.guns.AAGunType;
import com.flansmod.common.guns.AttachmentType;
import com.flansmod.common.guns.BulletType;
import com.flansmod.common.guns.GrenadeType;
import com.flansmod.common.guns.GunType;
import com.flansmod.common.guns.ItemAAGun;
import com.flansmod.common.guns.ItemAttachment;
import com.flansmod.common.guns.ItemBullet;
import com.flansmod.common.guns.ItemGrenade;
import com.flansmod.common.guns.ItemGun;
import com.flansmod.common.guns.boxes.BlockGunBox;
import com.flansmod.common.guns.boxes.GunBoxType;
import com.flansmod.common.parts.ItemPart;
import com.flansmod.common.parts.PartType;
import com.flansmod.common.teams.ArmourBoxType;
import com.flansmod.common.teams.ArmourType;
import com.flansmod.common.teams.BlockArmourBox;
import com.flansmod.common.teams.ItemRewardBox;
import com.flansmod.common.teams.ItemTeamArmour;
import com.flansmod.common.teams.RewardBox;
import com.flansmod.common.tools.ItemTool;
import com.flansmod.common.tools.ToolType;
import com.flansmod.common.types.EnumType;
import com.flansmod.common.types.InfoType;
import com.flansmod.common.types.TypeFile;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;

public class ContentManager 
{
	public class ContentPackMod implements IFlansModContentProvider
	{
		public IFlansModContentProvider provider;
		public ModContainer container;
		
		public ContentPackMod(ModContainer c, IFlansModContentProvider p)
		{
			container = c;
			provider = p;
		}
		@Override
		public String GetContentFolder() 
		{
			return provider.GetContentFolder();
		}
		
		@Override
		public void RegisterModelRedirects() 
		{
			provider.RegisterModelRedirects();
		}
	}
	
	@Deprecated
	public class ContentPackFlanFolder implements IFlansModContentProvider
	{
		public final String name;
		public final File folder;

		public ContentPackFlanFolder(String n, File f) 
		{ 
			folder = f; name = n; 
		}
		public ContentPackFlanFolder(File f) 
		{ 
			folder = f; name = f.getName(); 
		}
		
		@Override
		public String GetContentFolder() 
		{
			return name;
		}
		
		@Override
		public void RegisterModelRedirects() 
		{
			try
			{
				if(folder.isDirectory())
				{
					File redirectInfo = new File(folder, "/redirect.info");
					if(redirectInfo.exists())
					{
						BufferedReader reader = new BufferedReader(new FileReader(redirectInfo));
						String src = reader.readLine();
						String dst = reader.readLine();
						
						if(src != null && dst != null)
						{
							FlansMod.log.info("Registered Flan folder model redirect from " + src + " to " + dst);
							FlansMod.RegisterModelRedirect(src, dst);
						}
						
						reader.close();
					}
				}
				else if(zipJar.matcher(folder.getName()).matches())
				{
					ZipFile zip = new ZipFile(folder);
					ZipEntry entry = zip.getEntry("redirect.info");
					
					if(entry != null && !entry.isDirectory())
					{
						BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
						String src = reader.readLine();
						String dst = reader.readLine();
						
						if(src != null && dst != null)
						{
							FlansMod.log.info("Registered Flan folder model redirect from " + src + " to " + dst);
							FlansMod.RegisterModelRedirect(src, dst);
						}
						
						reader.close();
					}
					
					zip.close();
				}
			}
			catch(Exception e)
			{
				
			}
		}
	}
	
	//Side note: why is this not just a static class?
	//Actual fields for ContentManager instance
	/**
	 * List of all the content packs found.
	 * Key is the name of the pack, value is the Mod instance which implements IFlansModContentProvider
	 */
	private List<IFlansModContentProvider> packs = new ArrayList<IFlansModContentProvider>();
	protected Pattern zipJar = Pattern.compile("(.+)\\.(zip|jar)$");
	private boolean wasAnythingInFlanFolder = false;
	
	/**
	 * Checks whether or not Flan's mod has found a content pack
	 * Does not guarantee the content pack(s) were loaded, just if they were found
	 * @return true if a content pack was found, false otherwise
	 */
	public boolean LoadedAnyContentFromFlanFolder()
	{
		return wasAnythingInFlanFolder;
	}
	
	/**
	 * Finds all the content packs in the Flan folder
	 * The packs are loaded into the instance
	 */
	@Deprecated
	public void FindContentInFlanFolder()
	{
		for(File file : FlansMod.flanDir.listFiles())
		{
			//Load folders and valid zip files
			if(file.isDirectory() || zipJar.matcher(file.getName()).matches())
			{
				//Add the directory to the content pack list
				FlansMod.log.info("Loaded content pack from Flan folder : " + file.getName());
				//For legacy content packs, their name is simply their file 
				packs.add(new ContentPackFlanFolder(file));
				wasAnythingInFlanFolder = true;
			}
		}
		FlansMod.log.info("Loaded content pack list from Flan folder");
	}
	

	@Deprecated
	public void LoadAssetsFromFlanFolder()
	{
		FlansMod.proxy.LoadAssetsFromFlanFolder();
	}
	
	public void RegisterModelRedirects()
	{
		for(IFlansModContentProvider provider : packs)
			provider.RegisterModelRedirects();
	}
	
	/**
	 * Finds all the content packs within the Mod folder
	 * The packs are loaded into the instance
	 */
	public void FindContentInModsFolder()
	{
		// Search for content packs in the mods folder
		for(ModContainer container : Loader.instance().getActiveModList())
		{
			for(ArtifactVersion requirement : container.getRequirements())
			{
				if(requirement.getLabel().equals(FlansMod.MODID))
				{
					if(container.getMod() instanceof IFlansModContentProvider)
					{
						// This is a Flan's Mod dependency. Register it as a content pack
						IFlansModContentProvider mod = ((IFlansModContentProvider)container.getMod());
						
						FlansMod.log.info("Loading content pack: " + container.getName());
						packs.add(new ContentPackMod(container, mod));
					}
					else
					{
						FlansMod.log.error("Found possible Flan's Mod content pack (" + container.getName() + ") which did not implement IFlansModContentProvider. Ignore if mod listed is not a Flan's Mod content pack");
					}
					break; //No need to look through the other requirements, we only care about the Flans one
				}
			}
		}
	}
	
	/**
	 * Loads types from a folder into the instance
	 * Used during debugging to load the mod from bin
	 * @param contentPackName the name of the content pack we're loading from
	 * @param directory the directory we're loading from (should be .../bin/main, or something similar)
	 */
	private void LoadTypesFromDirectory(String contentPackName, File directory) 
	{
		
		for(EnumType typeToCheckFor : EnumType.values())
		{
			//Getting the directory of the type that we're interested in
			File typesDir = new File(directory, "/" + typeToCheckFor.folderName + "/");
			
			//Some packs may be missing some categories, make sure they exist
			if(typesDir.exists()) {
				
				//Going through every file in the directories and subdirectories
				//All Flan's Mod config files should have the .txt extension
				for(File file : FileUtils.listFiles(typesDir, new String[] {"txt"}, true))
				{

					//Adding type to TypeFile's files list
					TypeFile.addContentFromFile(contentPackName, typeToCheckFor, file);
				}
			}
		}
	}
	
	/**
	 * Loads types from a .zip or .jar file (most likle a .jar)
	 * @param contentPackName the name of the content pack we're loading from
	 * @param contentPack the file of the content pack (i.e., a File pointing to the .jar file)
	 */
	private void LoadTypesFromArchive(String contentPackName, File contentPack)
	{
		try
		{
			ZipFile zip = new ZipFile(contentPack);
            Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry zipEntry = entries.nextElement();
				
				if (!zipEntry.isDirectory()) {
					for(EnumType type : EnumType.values())
					{
						if(zipEntry.getName().startsWith(type.folderName) && zipEntry.getName().endsWith(".txt"))
						{
							TypeFile.addContentFromZipEntry(contentPackName, type, zip, zipEntry);
						}
					}
				}
			}
			
			zip.close();
		}
		catch(IOException e)
		{
			FlansMod.log.throwing(e);
		}
	}
	
	public void LoadTypes()
	{
		for(IFlansModContentProvider pack : packs)
		{
			//If pack was found in Flan folder
			if(pack instanceof ContentPackFlanFolder)
			{
				ContentPackFlanFolder contentPack = (ContentPackFlanFolder)pack;
				//If content pack is a folder
				if(contentPack.folder.isDirectory())
				{
					LoadTypesFromDirectory(contentPack.name, contentPack.folder);
				}
				else // Otherwise let's hope its a zip / jar
				{
					LoadTypesFromArchive(contentPack.name, contentPack.folder);
				}
			}
			else if(pack instanceof ContentPackMod)// Must be a mod in the classpath
			{
				ContentPackMod mod = (ContentPackMod)pack;
				File source = mod.container.getSource();
				if (source.isFile()) 
				{
					//Loading from a .jar/.zip
					LoadTypesFromArchive(mod.container.getName(), source);
				} 
				else if (source.isDirectory())
				{
					LoadTypesFromDirectory(mod.container.getName(), source);
				}
			}	
		}
	}
	
	public void CreateItems()
	{
		for(EnumType type : EnumType.values())
		{
			Class<? extends InfoType> typeClass = type.getTypeClass();
			for(TypeFile typeFile : TypeFile.files.get(type))
			{
				try
				{
					InfoType infoType = (typeClass.getConstructor(TypeFile.class).newInstance(typeFile));
					infoType.read(typeFile);
					switch(type)
					{
						case bullet: new ItemBullet((BulletType)infoType).setTranslationKey(infoType.shortName);
							break;
						case attachment: new ItemAttachment((AttachmentType)infoType).setTranslationKey(infoType.shortName);
							break;
						case gun: new ItemGun((GunType)infoType).setTranslationKey(infoType.shortName);
							break;
						case grenade: new ItemGrenade((GrenadeType)infoType).setTranslationKey(infoType.shortName);
							break;
						case part: FlansMod.partItems.add((ItemPart)new ItemPart((PartType)infoType).setTranslationKey(infoType.shortName));
							break;
						case plane: new ItemPlane((PlaneType)infoType).setTranslationKey(infoType.shortName);
							break;
						case vehicle: new ItemVehicle((VehicleType)infoType).setTranslationKey(infoType.shortName);
							break;
						case aa: new ItemAAGun((AAGunType)infoType).setTranslationKey(infoType.shortName);
							break;
						case mechaItem: new ItemMechaAddon((MechaItemType)infoType).setTranslationKey(infoType.shortName);
							break;
						case mecha: FlansMod.mechaItems.add((ItemMecha)new ItemMecha((MechaType)infoType).setTranslationKey(infoType.shortName));
							break;
						case tool: FlansMod.toolItems.add((ItemTool)new ItemTool((ToolType)infoType).setTranslationKey(infoType.shortName));
							break;
						case box: new BlockGunBox((GunBoxType)infoType).setTranslationKey(infoType.shortName);
							break;
						case armour: FlansMod.armourItems.add((ItemTeamArmour)new ItemTeamArmour((ArmourType)infoType).setTranslationKey(infoType.shortName));
							break;
						case armourBox: new BlockArmourBox((ArmourBoxType)infoType).setTranslationKey(infoType.shortName);
							break;
						case playerClass: break;
						case team: break;
						case itemHolder: new BlockItemHolder((ItemHolderType)infoType);
							break;
						case rewardBox: new ItemRewardBox((RewardBox)infoType).setTranslationKey(infoType.shortName);
							break;
						case loadout: break;
						case glove:
							new ItemGlove((GloveType)infoType);
							break;
						default: FlansMod.log.warn("Unrecognised type for " + infoType.shortName);
							break;
					}
				}
				catch(Exception e)
				{
					FlansMod.log.error("Failed to add " + type.name() + " : " + typeFile.name);
					FlansMod.log.throwing(e);
				}
			}
			FlansMod.log.info("Loaded " + type.name() + ".");
		}
	}

	public List<File> GetFolderContentPacks() 
	{
		List<File> result = new ArrayList<File>();
		for(IFlansModContentProvider pack : packs)
		{
			if(pack instanceof ContentPackFlanFolder)
			{
				ContentPackFlanFolder contentPack = (ContentPackFlanFolder)pack;
				if(contentPack.folder.isDirectory())
				{
					result.add(contentPack.folder);
				}
			}
			else if(pack instanceof ContentPackMod)
			{
				ContentPackMod mod = (ContentPackMod)pack;
				if(mod.container.getSource().getName().endsWith("bin"))
				{	
					// If loading from inside MCP, use the content name to find content in run directory
					result.add(new File(FlansMod.flanDir + "/" + pack));
				}
			}
		}
		return result;
	}
}
