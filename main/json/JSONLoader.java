package main.json;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class JSONLoader {

	public Node node = new Node();
	public String created, updated;
	public int uuid;
	
	public void load(File file) throws IOException {
		
		node.clear();
		
		byte[] bytes = new byte[(int) file.length()];
		
		BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		int i = 0;
		while(i < bytes.length){
			int di = inputStream.read(bytes, i, bytes.length-i);
			if(di < 0) break;
			i += di;
		}
		inputStream.close();
		
		i = 0;
		while(bytes[i] != '{'){
			i++;
		}
		
		load(bytes, i+1, node);
		
		// System.out.println(this);
	}
	
	public String parseString(byte[] bytes, int a, int b) throws UnsupportedEncodingException {
		for(int i=0;i<b;i++){
			if(bytes[a+i] == '\\'){
				return parseString(new String(bytes, a, b, "UTF-8"));
			}
		}
		return new String(bytes, a, b, "UTF-8");
	}
	
	public String parseString(String str){
		
		if(str.length() > 80000) return str;
		
		
		int startIndex = 0;
		while(true){
			
			int i = str.indexOf('\\', startIndex);
			if(i < 0) return str;
			
			switch(str.charAt(i+1)){
			case 'r':
				str = str.substring(0, i)+"\r"+str.substring(i+2);
				startIndex = i+1;
				break;
			case 'n':
				str = str.substring(0, i)+"\n"+str.substring(i+2);
				startIndex = i+1;
				break;
			case '\\':
				str = str.substring(0, i)+"\\"+str.substring(i+2);
				startIndex = i+1;
				break;
			case 't':
				str = str.substring(0, i)+"\t"+str.substring(i+2);
				startIndex = i+1;
				break;
			case '"':
				str = str.substring(0, i)+"\""+str.substring(i+2);
				startIndex = i+1;
				break;
			case 'u':
				str = str.substring(0, i)+(char)(Integer.parseInt(str.substring(i+2, i+6), 16))+str.substring(i+6);
				startIndex = i+1;
				break;
			
			default:
				System.out.println(str.substring(i-3));
				throw new RuntimeException("unknown escape sequence "+str.charAt(i+1)+" "+(int)str.charAt(i+1));
			}
			
		}
		
		
		
		// return str;
	}
	
	public int load(byte[] bytes, int start, Node node) throws IOException {
		// returns the end
		// "url":...
		String name = null;
		boolean hasDot2 = false;
		for(int i=start;i<bytes.length;i++){
			byte b = bytes[i];
			switch(b){
			case '[':
				if(name == null){
					throw new IOException("unexpected [");
				} else if(hasDot2){
					// alles mögliche kann im Array sein, Zahlen, Strings, Objekte
					
					int j = i, index = 0;
					Node newNode = new Node();
					node.values.put(name, newNode);
					name = null;
					hasDot2 = false;
					
					loop:while(true){
						switch(bytes[++j]){
						case '0':case '1':case '2':case '3':case '4':
						case '5':case '6':case '7':case '8':case '9':
						case '.':
							// get the number :)
							int k = j;
							loop2:while(true){
								switch(bytes[++k]){
								case '0':case '1':case '2':case '3':case '4':
								case '5':case '6':case '7':case '8':case '9':
								// case '.':
									break;
								case ' ':
								case '\t':
								case '\r':
								case '\n':
								case ']':
									break loop2;
								default:
									throw new IOException("unexpected char "+(char)bytes[k]+" in number");
								}
							}
							newNode.ints.put(index+"", Integer.parseInt(new String(bytes, j, k-j)));
							index++;
							j = k-1;
							break;
						case '"':
							// get the string
							k = j+1;
							while(true){
								if(bytes[k] == '"') break;
								else if(bytes[k] == '\n'){
									k+=2;
								} else k++;
							}
							newNode.strings.put(index+"", parseString(bytes, j, k-j));
							index++;
							j = k;
							break;
						case '{':
							// get the node
							Node newNode2 = new Node();
							newNode.values.put(index+"", newNode2);
							index++;
							j = load(bytes, j+1, newNode2)-1;
							break;
						case ' ':
						case '\r':
						case '\t':
						case '\n':
							break;
						case ']':
							i = j;
							newNode.ints.put("length", index);
							break loop;
						case ',':break;
						default: throw new IOException("unexpected char "+(char)bytes[j]+", "+this);
						}
					}
				} else throw new IOException("expected :");
				break;
			case '{':
				if(name == null){
					throw new IOException("unexpected {");
				} else if(hasDot2){
					Node newNode = new Node();
					node.values.put(name, newNode);
					i = load(bytes, i+1, newNode);
					hasDot2 = false;
					name = null;
				} else throw new IOException("expected :");
				break;
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				break;
			case '}':
				return i+1;
			case ':':
				hasDot2 = true;break;
			case '"':
				if(name == null || hasDot2){
					int j = i+1;
					while(true){
						if(bytes[j] == '"') break;
						else if(bytes[j] == '\\'){
							j+=2;
						} else j++;
					}
					if(name != null){
						node.strings.put(name, parseString(bytes, i+1, j-(i+1)));
						name = null;
						hasDot2 = false;
					} else {
						name = parseString(bytes, i+1, j-(i+1));
					}
					i = j;
					break;
				} else throw new IOException("unexpected \": "+parseString(bytes, i-5, 10));
			case 't':
				i += 4-1;
				node.strings.put(name, "true");
				name = null;
				hasDot2 = false;
				break;
			case 'f':
				i += 5-1;
				node.strings.put(name, "false");
				name = null;
				hasDot2 = false;
				break;
			case ',':break;
			case '0':case '1':case '2':case '3':case '4':
			case '5':case '6':case '7':case '8':case '9':
				if(hasDot2 && name != null){
					
					int j = i;
					loop:while(true){
						switch(bytes[++j]){
						case '0':case '1':case '2':case '3':case '4':
						case '5':case '6':case '7':case '8':case '9':
						// case '.':
							break;
						case ',':
						case ' ':
						case '\t':
						case '\r':
						case '\n':
							break loop;
						}
					}
					
					node.ints.put(name, Integer.parseInt(new String(bytes, i, j-i)));
					name = null;
					hasDot2 = false;
					
					i = j;
					
					break;
				} else throw new IOException("expected :");
			default:
				throw new IOException("unexpected character: "+(char) bytes[i]+", "+this);
			}
		}
		
		return bytes.length;
	}
	
	@Override
	public String toString() {
		return node.toString();
	}
	
	public static class Node {
		
		public String getString(String name, String defaultValue){
			String answer = strings.get(name);
			if(answer != null) return answer;
			Integer intAnswer = ints.get(name);
			if(intAnswer != null) return intAnswer+"";
			else return defaultValue;
		}

		public int getInt(String name, int defaultValue) {
			Integer intAnswer = ints.get(name);
			if(intAnswer != null) return intAnswer;
			String answer = strings.get(name);
			if(answer != null) return Integer.parseInt(answer);
			else return defaultValue;
		}

		public HashMap<String, Integer> ints = new HashMap<>();
		public HashMap<String, String> strings = new HashMap<>();
		public HashMap<String, Node> values = new HashMap<String, JSONLoader.Node>();
		
		void clear(){
			strings.clear();
			values.clear();
		}
		
		@Override
		public String toString() {
			return strings+", "+values;
		}
	}
}
