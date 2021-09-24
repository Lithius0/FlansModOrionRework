package com.flansmod.common.types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.flansmod.common.FlansMod;

public class TypeFile
{
	/**
	 * All the files currently loaded, organized by Type
	 */
	public final static HashMap<EnumType, ArrayList<TypeFile>> files;
	
	/**
	 * The type of the content within this file
	 */
	public final EnumType type;
	public final String name, contentPack;
	/**
	 * A list of all the non-comment lines within the file, in order
	 */
	private ArrayList<String> configLines;
	/**
	 * The current position the reader is in, increments forward when readLine is called
	 * Prevents the parsers from reading the same line twice
	 */
	private int readerPosition = 0;
	private int hash = 0x12345678;
	
	static
	{
		files = new HashMap<EnumType, ArrayList<TypeFile>>();
		for(EnumType type : EnumType.values())
		{
			files.put(type, new ArrayList<>());
		}
		
	}
	
	public TypeFile(String contentPack, EnumType type, String name)
	{
		this.type = type;
		this.name = name;
		this.contentPack = contentPack;
		configLines = new ArrayList<>();
	}
	
	/**
	 * Adds a Flans object from an uncompressed .txt file
	 * The file is then processed into a list of Strings for easier parsing later
	 * @param contentPack 	the name of the content pack
	 * @param type 			the type of the 
	 * @param contentFile	the .txt file
	 */
	public static void addContentFromFile(String contentPack, EnumType type, File contentFile)
	{
		//Getting the name of the file without the extension
		String fileName = contentFile.getName().split("\\.")[0];
		
		TypeFile newFile = new TypeFile(contentPack, type, fileName);
				
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(contentFile));

			newFile.parseFromReader(reader);
			
			reader.close();
			
			//If the entire file read properly, we'll go ahead and add it to the list of files
			//If the file threw an exception, it's simply not added to the list
			files.get(type).add(newFile);
		}
		catch(IOException e)
		{
			FlansMod.log.throwing(e);
		}
	}

	
	/**
	 * Adds a Flans object from a zip entry (a file in a .zip or .jar archive)
	 * The file is then processed into a list of Strings for easier parsing later
	 * @param contentPack the name of the contentPack
	 * @param type the type of the Flans object (gun, bullet, plane, etc)
	 * @param packFile the archive file
	 * @param contentEntry the specific archive entry that points to the object to load
	 */
	public static void addContentFromZipEntry(String contentPack, EnumType type, ZipFile packFile, ZipEntry contentEntry)
	{
		//Getting the name of the file without the extension
		String[] splitName = contentEntry.getName().split("/"); 
		String fileName = splitName[splitName.length - 1]; //Get the name of the text file from the path
		fileName = fileName.split("\\.")[0]; //Chopping off the extension

		TypeFile newFile = new TypeFile(contentPack, type, fileName);
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(packFile.getInputStream(contentEntry)));
			
			newFile.parseFromReader(reader);
			
			reader.close();
			
			//If the entire file read properly, we'll go ahead and add it to the list of files
			//If the file threw an exception, it's simply not added to the list
			files.get(type).add(newFile);
		}
		catch(IOException e)
		{
			FlansMod.log.throwing(e);
		}
	}
	
	/**
	 * Adds a string to be processed later. 
	 * This method will also remove any commented text (i.e. text after a //) and ignore empty lines
	 * @param line the string to be added
	 */
	private void addLineForParsing(String line)
	{
		//Primarily to remove empty lines that happen to have a space
		//Line with just a " " would not trigger the isEmpty check
		line = line.trim(); 
		
		//Remove comments
		int commentLocation = line.indexOf("//");
		if (commentLocation >= 0) 
		{
			line = line.substring(0, commentLocation);
		}
		
		//If there's a comment, it's likely to be the entire line that's commented out. 
		//Also some lines will be empty for the sake of spacing and readability in the config file
		//Only add if we still have something left to add
		if (!line.isEmpty()) {
			configLines.add(line);
			hash ^= line.hashCode();
		}
	}
	
	/**
	 * Reads text with a BufferedReader and adds the line to the list for parsing
	 * @param reader the reader to read from
	 * @throws IOException If reader throws an exception. If BufferedReader will throw an exception, this will too
	 */
	private void parseFromReader(BufferedReader reader) throws IOException 
	{

		String line = reader.readLine();
		//Read until end of file (reader returns null at end of file)
		while(line != null)
		{
			this.addLineForParsing(line);
			line = reader.readLine();
		}
	}
	
	/**
	 * Gets the next line from the cleaned-up list of configs and advances the reader position by 1
	 * @return the next line
	 */
	public String readLine()
	{
		if(readerPosition == configLines.size())
			return null;
		return configLines.get(readerPosition++);
	}
	
	/**
	 * Get the full list of config lines
	 * @return the full list of config lines
	 */
	public List<String> getLines()
	{
		return configLines;
	}
	
	@Override
	public int hashCode()
	{
		return hash;
	}
}
