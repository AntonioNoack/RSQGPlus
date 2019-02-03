package main.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ImageDataLoader {
	
	public HashMap<String, String> data = new HashMap<>();
	
	private String[] split(String src){
		src = src.substring(1, src.length()-2);
		return src.split("\",\"");
	}
	
	public void load(File file) throws IOException {
		
		data.clear();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String[] attributes = split(reader.readLine().toLowerCase());
		String valueText = reader.readLine(), nextLine;
		while((nextLine = reader.readLine()) != null){
			valueText += "\n"+nextLine;
		}
		String[] values = split(valueText);
		for(int i=0,l=Math.min(attributes.length, values.length);i<l;i++){
			data.put(attributes[i], values[i]);
		}
		reader.close();
		
	}
}
