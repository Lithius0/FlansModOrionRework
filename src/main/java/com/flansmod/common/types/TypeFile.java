package com.flansmod.common.types;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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
	private ArrayList<String> rawLines;
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
		rawLines = new ArrayList<>();
	}
	
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
	
	private void parseLine(String line)
	{
		//TODO: Do basic things like remove comments here
		rawLines.add(line);
		hash ^= line.hashCode();
	}
	
	private void parseFromReader(BufferedReader reader) throws IOException 
	{

		String line = reader.readLine();
		//Read until end of file (reader returns null at end of file)
		while(line != null)
		{
			this.parseLine(line);
			line = reader.readLine();
		}
	}
	
	public String readLine()
	{
		if(readerPosition == rawLines.size())
			return null;
		return rawLines.get(readerPosition++);
	}
	
	public List<String> getLines()
	{
		return rawLines;
	}
	
	@Override
	public int hashCode()
	{
		return hash;
	}
}
