package main.json;

import java.io.IOException;

import main.Structure;
import main.json.JSONLoader.Node;

public class Creator {
	
	public static int uuidCtr = 1;
	
	public int uuid, iconId;
	public String name;

	public Creator(Structure structure, Node node) throws IOException {
		
		name = node.getString("displayName", "?");
		uuid = uuidCtr++;
		
		iconId = structure.registerImage(node.getString("avatarImageUrl", null), false);
		
	}
	
}
