package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JFileChooser;

import main.Main.State;
import main.csv.ImageDataLoader;
import main.json.Creator;
import main.json.JSONLoader;
import main.json.JSONLoader.Node;

public class Structure {
	
	public static final File configFolder = new File(System.getProperty("user.home"), ".AntonioNoack/RSQGPlus/");
	public static final File configFile = new File(configFolder, "config.txt");
	
	public HashMap<String, String> imagePath = new HashMap<>();
	public HashMap<String, Integer> images = new HashMap<>();
	public HashMap<Integer, String> rImages = new HashMap<>();
	
	public ArrayList<JSONLoader> posts = new ArrayList<>();
	public HashMap<String, Creator> creators = new HashMap<>();
	
	public HashMap<String, Integer> resourceMap = new HashMap<>();
	
	public HashMap<String, String> config = new HashMap<>();
	
	public File cacheFile, sourceFile;

	public File askFile(String title){
		JFileChooser jfc = new JFileChooser();
		// jfc.showDialog(null, title);
		jfc.showSaveDialog(null);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setVisible(true);
		return jfc.getCurrentDirectory();
	}
	
	public Structure(){
		
		rImages.put(0, "0");
		images.put("0", 0);
		
		if(!configFolder.exists() || !configFile.exists() || configFile.length() < 5){
			
			redoConfig();
			
		} else {
			try {
				BufferedReader read = new BufferedReader(new FileReader(configFile));
				String line;
				while((line = read.readLine()) != null){
					int index = line.indexOf(':');
					if(index > -1){
						config.put(line.substring(0, index).trim(), line.substring(index+1).trim());
					}
				}
				read.close();
				
				if(config.get("Data-Location") != null){
					sourceFile = new File(config.get("Data-Location"));
					if(sourceFile.exists() && sourceFile.isDirectory()){
						
						if(config.get("Image-Cache") != null){
							cacheFile = new File(config.get("Image-Cache"));
							if(!cacheFile.exists() || !cacheFile.isDirectory()) cacheFile = null;
						}
						
						if(cacheFile == null){
							cacheFile = new File(sourceFile, "cache");
							cacheFile.mkdirs();
						}
						
						if(config.get("Port") != null){
							try {
								Main.port = Integer.parseInt(config.get("Port"));
							} catch(NumberFormatException e){
								Main.port = 8080;
							}
						}
						
					} else redoConfig();
				}
				
			} catch(IOException e){
				e.printStackTrace();
			}
		}
		
		try {
			create();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void redoConfig(){
		
		configFolder.mkdirs();
		// write config file...
		// ask the user for the input...
		
		sourceFile = askFile("Select the posts/Beiträge folder!");
		Main.port = Main.askNumber("What shall the port be? Default: 8080", 8080);
		if(Main.port < 0 || Main.port > 65535) Main.port = 8080;
		Main.repaint();
		
		writeConfig();
		cacheFile = new File(sourceFile, "cache");
		
	}
	
	public void writeConfig(){
		
		if(cacheFile == null){
			cacheFile = new File(sourceFile, "cache");
		}
		
		try {
			FileWriter writer = new FileWriter(configFile);
			writer.write("Data-Location: "+sourceFile.getAbsolutePath()+"\n");
			writer.write("Image-Cache: "+cacheFile+"\n");
			writer.write("Port: "+Main.port+"\n");
			if(!cacheFile.exists()){
				cacheFile.mkdirs();
				FileWriter writer2 = new FileWriter(new File(cacheFile, "README.txt"));
				writer2.write("Here are the profile images saved for the Resque-Google-Plus project from Antonio Noack (from Jena, Germany)\n");
				writer2.close();
			}
			writer.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public String compressGoogleURL(String url){
		int frontStart = url.indexOf('/', "https://".length()+1)+1;
		int backStart = url.lastIndexOf('/')+1;
		int frontEnd = url.indexOf('/', frontStart);
		if(frontEnd < 0) return url.substring(frontStart);
		return url.substring(frontStart, frontEnd) + "/" + url.substring(backStart);
	}
	
	public int registerImage(String url, boolean large) throws IOException {
		
		if(url == null) return -1;
		
		Integer index = images.get(url);
		if(index != null){
			
			return index;
		}
		
		String mapped = imagePath.get(url);
		if(mapped == null && large){
			mapped = imagePath.get(compressGoogleURL(url));
		}
		
		if(mapped == null){
			
			File cached1 = new File(cacheFile, Integer.toHexString(url.hashCode())+".png");
			File cached2 = new File(cacheFile, Integer.toHexString(url.hashCode())+".jpg");
			File cached3 = new File(cacheFile, Integer.toHexString(url.hashCode())+".gif");
			File cached;
			if(!cached1.exists() && !cached2.exists()){
				// needs to be queried and cached...
				if(url.startsWith("//lh")) url = "https:"+url;
				try {
					HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
					connection.setRequestMethod("GET");
					connection.setDoOutput(false);
					
					int code = connection.getResponseCode();
					
					if(code == 200){
						
						InputStream in = new BufferedInputStream(connection.getInputStream());

						byte[] buffer = new byte[1024];
						int i = 0;
						while(true){
							int di = in.read(buffer, i, buffer.length-i);
							if(di < 0) break;
							i += di;
							if(i > 3) break;
						}
						
						cached = (buffer[1] == 'P' && buffer[2] == 'N' && buffer[3] == 'G') ? cached1 : (buffer[0] == 'G' && buffer[1] == 'I' && buffer[2] == 'F') ? cached3 : cached2;
						OutputStream out = new BufferedOutputStream(new FileOutputStream(cached));
						out.write(buffer, 0, i);
						
						while(true){
							int len = in.read(buffer);
							if(len < 0) break;
							out.write(buffer, 0, len);
						}
						
						out.close();
						in.close();
						
					} else {// 404 :(
						(cached = cached1).createNewFile();
						System.out.println(url+" not found :(");
					}
				} catch(UnknownHostException e){
					(cached = cached1).createNewFile();
					System.out.println(url+" not found :(");
				}
			} else {
				cached = cached1.exists() ? cached1 : cached2.exists() ? cached2 : cached3;
			}
			
			int ix = images.size();
			images.put(url, ix);
			rImages.put(ix, cached.getAbsolutePath());
			imagePath.put(ix+"", cached.getAbsolutePath());
			
			return ix;
			
		} else {
			
			// found in folder
			int ix = images.size();
			images.put(url, ix);
			rImages.put(ix, mapped);
			imagePath.put(ix+"", mapped);
			
			return ix;
			
		}
	}
	
	private void registerCreator(Node node){
		if(node != null){
			String resName = node.getString("resourceName", null);
			if(resName != null){
				if(creators.get(resName) == null){
					try {
						creators.put(resName, new Creator(this, node));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void create() throws IOException {
		
		Main.state = State.LOADING_IMAGES;
		Main.repaint();
		
		ImageDataLoader csv = new ImageDataLoader();
		
		File loadingDir = new File("/home/antonio/Downloads/tske/Takeout/str/Beiträge/");
		
		File[] files = loadingDir.listFiles();
		Main.maximal = 0;
		for(File load: files){
			String name = load.getAbsolutePath();
			if(name.endsWith(".csv")){
				Main.maximal++;
			}
		}
		
		Main.position = 0;
		// first load all images
		for(File load: files){
			String name = load.getAbsolutePath();
			if(name.endsWith(".csv")){
				
				int ix = name.indexOf(".", name.indexOf(".")+1);
				if(ix > -1){
					csv.load(load);
					String url = csv.data.get("url");
					String imageFileName = name.substring(0, ix);
					if(new File(imageFileName).exists()){
						imagePath.put(url, imageFileName);
						imagePath.put(compressGoogleURL(url), imageFileName);
					} else System.out.println("image "+imageFileName+" not found... crazy...");
				}// else Metadaten(33).csv, was auch immer das soll...
				
				Main.position++;
				Main.repaint();
			}
		}
		
		Main.state = State.LOADING_POSTS;
		Main.position = 0;
		Main.maximal = 0;
		Main.repaint();
		
		// then load all posts
		ArrayList<JSONLoader> posts = new ArrayList<JSONLoader>();
		for(File load: loadingDir.listFiles()){
			String name = load.getName();
			if(name.endsWith(".json")){
				
				Main.maximal++;
				Main.repaint();

				JSONLoader json = new JSONLoader();
				json.load(load);
				json.created = json.node.getString("creationTime", "");
				json.updated = json.node.getString("updateTime", "");
				posts.add(json);
				
			}
		}
		
		registerPostData(posts, true, 5);
		
		Collections.sort(this.posts, new Comparator<JSONLoader>() {
			@Override public int compare(JSONLoader s, JSONLoader t) {
				return t.created.compareTo(s.created);
			}
		});
		
		int ctr = 0;
		for(JSONLoader loader: this.posts){
			loader.uuid = ctr;
			resourceMap.put(loader.node.strings.get("resourceName"), loader.uuid);
			ctr++;
		}
		
		System.out.println("People you have interacted with: "+creators.size());
		
	}
	
	public void registerPostData(ArrayList<JSONLoader> posts, boolean doCount, int depth){
		
		ArrayList<JSONLoader> resharedPosts = new ArrayList<>();
		
		int jsonCtr = 0;
		int jsonCount = posts.size();
		
		for(JSONLoader json: posts){
			
			Main.position++;
			Main.repaint();
			
			registerCreator(json.node.values.get("author"));
			
			Node reshared = json.node.values.get("resharedPost");
			if(reshared != null){
				if(!(reshared.values.get("author")+"").equals(json.node.values.get("author")+"")){
					// todo add the post to the list, so it can be seen...
					JSONLoader load2 = new JSONLoader();
					load2.node = reshared;
					load2.created = reshared.getString("creationTime", "");
					load2.updated = reshared.getString("updateTime", "");
					resharedPosts.add(load2);
				}
			}
			
			Node comments = json.node.values.get("comments");
			if(comments != null){
				for(int i=0,l=comments.getInt("length", 0);i<l;i++){
					registerCreator(comments.values.get(i+"").values.get("author"));
				}
			}
			
			Node plusses = json.node.values.get("plusOnes");
			if(plusses != null){
				for(int i=0,l=plusses.getInt("length", 0);i<l;i++){
					registerCreator(plusses.values.get(i+"").values.get("plusOner"));
				}
			}

			jsonCtr++;
			if(20*jsonCtr/jsonCount > 20*(jsonCtr-1)/jsonCount){
				System.out.println("Posts["+depth+"]: "+jsonCtr+" / "+jsonCount);
			}
			
		}
		
		this.posts.addAll(posts);
		
		if(--depth < 0) return;
		if(resharedPosts.size() > 0){
			
			Main.maximal += resharedPosts.size();
			Main.repaint();
			
			registerPostData(resharedPosts, doCount, depth);
			
		}
	}
	
}
