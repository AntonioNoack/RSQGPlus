package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import main.Main.State;
import main.json.Creator;
import main.json.JSONLoader;
import main.json.JSONLoader.Node;

public class Server {
	
	private int port;
	
	private final Structure structure;
	
	private byte[][] template;
	
	public Server(Structure structure, int port){
		
		this.port = port;
		this.structure = structure;
		
		try {
			template = loadBytesSplit("template");
		} catch(IOException e){
			e.printStackTrace();
		}
		
		stringBuffer = null;
	}

	private byte[] stringBuffer = new byte[65536];
	private String loadString(String name) throws IOException {
		
		int i = 0;
		InputStream in2 = getClass().getResourceAsStream("/raw/templates/"+name+".txt");
		if(in2 == null) throw new IOException("Resource /raw/templates/"+name+".txt not found!");
		
		try {
			while(i < stringBuffer.length){
				int di = in2.read(stringBuffer, i, stringBuffer.length-i);
				if(di < 0) break;
				i += di;
			} 
		} catch(IOException e){
			System.out.println("/raw/templates/"+name+".txt"+" closed at length "+i);
			e.printStackTrace();
		}
		
		in2.close();
		
		return new String(stringBuffer, 0, i);
		
	}
	
	/*private byte[] loadBytes(String name) throws IOException {
		return loadString(name).getBytes("UTF-8");
	}*/
	
	private byte[][] loadBytesSplit(String name) throws IOException {
		String[] strings = loadString(name).split("###");
		byte[][] ret = new byte[strings.length][];
		for(int i=0;i<ret.length;i++){
			ret[i] = strings[i].getBytes("UTF-8");
		}
		return ret;
	}
	
	private long now = new Date().getTime();
	
	public long formatTime(String stringDate){
		try {
			if(stringDate == null || stringDate.length() < 1) return -1;
			long delta = 0;
			char spec = stringDate.charAt("2015-02-06 07:36:49".length());
			if(spec == '+' || spec == '-'){
				delta += 3600000 * Integer.parseInt(stringDate.substring("2015-02-06 07:36:49".length(), "2015-02-06 07:36:49+01".length()));
				stringDate = stringDate.substring(0, "2015-02-06 07:36:49".length());
			}
			long date = parseDate(stringDate) + delta;
			if(date < 0) return -1;
			else return (now - date)/1000;
		} catch(NumberFormatException e){
			System.out.println(stringDate);
			throw e;
		}
	}
	
	// todo if reshared append <reshare>
	
	public Creator getCreator(Node post){
		return structure.creators.get(post.values.get("author").strings.get("resourceName"));
	}
	
	public void printEntryBot(OutputStream out, Node post, int id, int likes, HashSet<Creator> alreadySent){
		
	}
	
	public void base64_encode(OutputStream out, String value) throws IOException {
		base64_encode(out, value.getBytes("UTF-8"));
	}
	
	public String base64_encode(String value) throws UnsupportedEncodingException {
		return base64_encode(value.getBytes("UTF-8"));
	}
	
	char[] base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	public void base64_encode(OutputStream out, byte[] value) throws IOException {
		int i;
		for(i=0;i+2<value.length;i+=3){
			int data = ((value[i] & 255) << 16) | ((value[i+1] & 255) << 8) | (value[i+2] & 255);
			out.write(base64Chars[(data >> 18) & 63]);
			out.write(base64Chars[(data >> 12) & 63]);
			out.write(base64Chars[(data >> 6) & 63]);
			out.write(base64Chars[(data) & 63]);
		}
		if(i < value.length){
			if(i+1 < value.length){// 16
				int data = ((value[i] & 255) << 16) | ((value[i+1] & 255) << 8);
				out.write(base64Chars[(data >> 18) & 63]);
				out.write(base64Chars[(data >> 12) & 63]);
				out.write(base64Chars[(data >> 6) & 63]);
				out.write('=');
			} else {// 8
				int data = ((value[i] & 255) << 16);
				out.write(base64Chars[(data >> 18) & 63]);
				out.write(base64Chars[(data >> 12) & 63]);
				out.write('=');
				out.write('=');
			}
		}// 0 = done
	}
	
	public String base64_encode(byte[] value){
		String ret = "";
		int i;
		for(i=0;i+2<value.length;i+=3){
			int data = ((value[i] & 255) << 16) | ((value[i+1] & 255) << 8) | (value[i+2] & 255);
			ret += (base64Chars[(data >> 18) & 63]);
			ret += (base64Chars[(data >> 12) & 63]);
			ret += (base64Chars[(data >> 6) & 63]);
			ret += (base64Chars[(data) & 63]);
		}
		if(i < value.length){
			if(i+1 < value.length){// 16
				int data = ((value[i] & 255) << 16) | ((value[i+1] & 255) << 8);
				ret += (base64Chars[(data >> 18) & 63]);
				ret += (base64Chars[(data >> 12) & 63]);
				ret += (base64Chars[(data >> 6) & 63]);
				ret += ('=');
			} else {// 8
				int data = ((value[i] & 255) << 16);
				ret += (base64Chars[(data >> 18) & 63]);
				ret += (base64Chars[(data >> 12) & 63]);
				ret += ('=');
				ret += ('=');
			}
		}// 0 = done
		return ret;
	}
	
	public long parseDate(String date){
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try {
			return dateFormat.parse(date).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public String uploadMedia(Node media) throws IOException {
		return structure.registerImage(media.getString("url", null), true)+"";
	}
	
	public String catchVideos(Node media) throws IOException {
		if(media.getString("contentType", "").indexOf("image/") == 0){
			return uploadMedia(media);
		} else {
			return base64_encode(media.getString("contentType", "")+"///"+media.getString("url", "")+"///"+media.getString("description", ""));
		}
	}
	
	public void printEntry(OutputStream out, Node post, int id, int likes, HashSet<Creator> alreadySent) throws IOException {
		
		Creator creator = getCreator(post);
		if(!alreadySent.contains(creator)){
			alreadySent.add(creator);
			out.write('3');
			out.write(',');
			out.write(fromInt(creator.uuid).getBytes());
			out.write(',');
			base64_encode(out, creator.name);
			out.write(',');
			out.write(fromInt(creator.iconId).getBytes());
			out.write(';');
		}
		
		String party = null;
		if(id > -1){// wenn es eine Id hat, dann vielleicht auch eine Communitygruppe
			Node postAcl = post.values.get("postAcl");
			if(postAcl != null){
				Node collectionAcl = postAcl.values.get("collectionAcl");
				if(collectionAcl != null){
					Node collection = collectionAcl.values.get("collection");
					party = collection.getString("displayName", null);
				} else {
					Node communityAcl = postAcl.values.get("communityAcl");
					// if it contains users, those were selected to be able to see it
					if(communityAcl != null){
						Node community = communityAcl.values.get("community");
						if(community != null){
							party = community.getString("displayName", null);
						}
					}
				}
			}
		}
		
		out.write(id > -1 ? '1' : '2');
		out.write(',');
		out.write(fromInt(id).getBytes());
		out.write(',');
		out.write(fromInt(creator.uuid).getBytes());
		out.write(',');
		
		String content = post.getString("content", "");
		
		// linked
		Node link = post.values.get("link");
		if(link != null){
			content += "<link>"+base64_encode(link.getString("title", ""))+"/"+link.getString("url", "");
		}
		
		// reshared
		Node reshare = post.values.get("resharedPost");
		if(reshare != null){
			Integer index = structure.resourceMap.get(reshare.getString("resourceName", null));
			if(index != null){
				content += "<reshare>"+index;
			} else System.out.println(reshare.getString("resourceName", null)+" has no post id :(");
		}
		
		base64_encode(out, content);
		out.write(',');
		
		// images
		Node album = post.values.get("album");
		if(album != null){
			Node media = album.values.get("media");
			if(media != null){
				boolean hadFirstMedia = false;
				for(int i=0,l=media.getInt("length", 0);i<l;i++){
					String image = catchVideos(media.values.get(i+""));
					if(image != null){
						if(hadFirstMedia){
							out.write('.');
						} else {hadFirstMedia = true;}
						out.write(image.getBytes("UTF-8"));
					}
				}
			} else {
				media = post.values.get("media");
				if(media != null){
					String image = catchVideos(media);
					if(image != null){
						out.write(image.getBytes("UTF-8"));
					}
				}
			}
		} else {
			Node media = post.values.get("media");
			if(media != null){
				String image = catchVideos(media);
				if(image != null){
					out.write(image.getBytes("UTF-8"));
				}
			}
		}
		// todo send image ids, concat by .
		out.write(',');
		if(id > -1 && party != null){
			base64_encode(out, party);
		}
		out.write(',');
		out.write((formatTime(post.getString("creationTime", null))+"").getBytes());
		out.write(',');
		out.write((formatTime(post.getString("updateTime", null))+"").getBytes());
		if(id > -1){
			out.write(',');
			out.write(fromInt(likes).getBytes());
		}
		out.write(';');
	}
	
	private String[] fromInt;
	private String fromInt(int value){
		if(fromInt == null){
			fromInt = new String[64];
			for(int i=0;i<fromInt.length;i++){
				fromInt[i] = i+"";
			}
		}
		if(value > -1 && value < fromInt.length){
			return fromInt[value];
		} else return value+"";
	}
	
	public void printEntries(OutputStream out, int id, int max, boolean isBot, HashSet<Creator> alreadySent) throws IOException {
		now = new Date().getTime();
		if(id < structure.posts.size()){
			JSONLoader post = structure.posts.get(id);
			if(post != null){
				Node likeNode = post.node.values.get("plusOnes");
				int likes = 0;
				if(likeNode != null){
					likes = Integer.parseInt(likeNode.getString("length", "0"));
				}
				Node comments = post.node.values.get("comments");
				if(isBot){
					printEntryBot(out, post.node, id, likes, alreadySent);
					if(comments != null){
						for(int i=0,l=comments.getInt("length", 0);i<l&&i<max;i++){
							printEntryBot(out, comments.values.get(fromInt(i)), -1, 0, alreadySent);
						}
					}
				} else {
					printEntry(out, post.node, id, likes, alreadySent);
					if(comments != null){
						for(int i=0,l=comments.getInt("length", 0);i<l&&i<max;i++){
							printEntry(out, comments.values.get(fromInt(i)), -1, 0, alreadySent);
						}
					}
				}
			}
		}
	}
	
	public String textPreview(String txt, int len){
		while(true){
			int i = txt.indexOf('<');
			if(i < 0) break;
			int j = txt.indexOf('>', i+1);
			if(j < 0) break;
			txt = txt.substring(0, i)+" "+txt.substring(j+1);
		}
		int l = txt.length();
		if(l < len) return txt;
		else {
			int j = txt.indexOf(' ', len);
			if(j < 0) return txt;
			return txt.substring(0, j);
		}
	}
	
	public void showDefault(OutputStream out, boolean isBot) throws IOException {
		
		now = new Date().getTime();
		
		if(isBot){
			
			out.write(template[0]);
			// todo link all entries...
			for(JSONLoader post: structure.posts){
				out.write(("<a href='?p="+post.uuid+"'>"+textPreview(post.node.getString("content", ""), 50)+"</a>").getBytes("UTF-8"));
			}
			out.write(template[1]);
			out.write("main.js".getBytes());
			out.write(template[2]);
			
		} else {
			
			out.write(template[0]);
			out.write(template[1]);
			out.write("main.js".getBytes());
			out.write(template[2]);
			
		}
	}
	
	public void sendHeader(OutputStream out, String mimeType) throws IOException {
		out.write(("HTTP/1.1 200 OK\r\nServer: RSQGPlus by Antonio Noack\r\nConnection: close\r\nContent-Type: "+mimeType+"\r\n\r\n").getBytes());
	}
	
	public void sendImage(OutputStream out, InputStream in) throws IOException {
		
		sendHeader(out, "image/jpeg");
		
		InputStream in2 = in instanceof BufferedInputStream ? in : new BufferedInputStream(in);
		byte[] buffer = new byte[1024];
		while(true){
			int di = in2.read(buffer);
			if(di < 0) break;
			out.write(buffer, 0, di);
		}
		
	}
	
	public void sendOK(OutputStream out) throws IOException {
		out.write(("HTTP/1.1 200 OK\r\nServer: RSQGPlus by Antonio Noack\r\nConnection: close\r\nContent-Type: text/html\r\n\r\n").getBytes());
	}
	
	private ServerSocket socket = null;
	public void listenAlways(){
		new Thread(new Runnable() {
			@Override public void run() {
				
				OutputStream out = new OutputStream() {
					@Override public void write(int arg0) throws IOException {}
					@Override public void write(byte[] arg0) throws IOException {}
					@Override public void write(byte[] arg0, int arg1, int arg2) throws IOException {}
				};
				
				for(int i=0;i<structure.posts.size();i+=100){
					final int i0 = i;
					new Thread(new Runnable() {
						@Override public void run() {
							try {
								for(int i=0;i<100 && i+i0<structure.posts.size();i++){
									printEntries(out, i+i0, 2000, false, new HashSet<>());
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
				
				try {
					
					socket = new ServerSocket(port);
					
					Main.state = State.READY;
					Main.repaint();
					
					while(true){
						final Socket client = socket.accept();
						new Thread(new Runnable() {
							@Override public void run() {
								try {
									
									InputStream in = new BufferedInputStream(client.getInputStream());
									OutputStream out = new BufferedOutputStream(client.getOutputStream());
									
									byte[] line1 = new byte[512];
									int i = 0;
									reading:while(i < line1.length){
										
										int di = in.read(line1, i, line1.length - i);
										if(di < 0) break;
										
										for(int j=i;j<i+di-3;j++){
											if(line1[j+0] == '\r' && line1[j+1] == '\n' && line1[j+2] == '\r' && line1[j+3] == '\n' ){
												i += di;break reading;
											}
										}
										
										for(int j=i;j<i+di-1;j++){
											if(line1[j] == '\n' && line1[j-1] == '\n'){
												i += di;break reading;
											}
										}
										
										i += di;
									}
									
									String[] args = new String(line1, 0, i).split("\n")[0].split(" ")[1].replace('?', '&').split("\\&");
									
									String file = args[0];
									if(file.equals("/")){
										
										// direct request :)
										boolean isBot = false;
										
										if(args.length > 1){
											
											String[] a1 = args[1].split("=");
											String a1Name = a1[0], a1Value = a1[1];
											
											switch(a1Name){
											case "pp":
												String rsp = structure.imagePath.get(a1Value);
												if(rsp != null){
													sendImage(out, new FileInputStream(new File(rsp)));
												} else {
													out.write(("HTTP/1.1 302 Found\r\nServer: RSQGPlus by Antonio Noack\r\nLocation: img/no.pp.png\r\nConnection: close\r\nContent-Type: text/html\r\n\r\n").getBytes());
												};break;
											case "pcp":
											case "pc":
											case "ps":
												rsp = structure.imagePath.get(a1Value);
												if(rsp != null){
													sendImage(out, new FileInputStream(new File(rsp)));
												} else {
													out.write(("HTTP/1.1 404 Not found!\r\n\r\n").getBytes());
												};break;
											case "qs":
												
												int perSite = 24;// todo change to 24, teilbar durch 2, 3, 4
												int site = Integer.parseInt(a1Value);
												int offset = site * perSite;
												// todo get id of logged in :)
												// change creator = 1 to his id -> he can see his threads

												sendOK(out);
												HashSet<Creator> alreadySent = new HashSet<Creator>();
												int j=0;
												i=offset;
												for(;i<structure.posts.size() && j<perSite;i++,j++){
													printEntries(out, i, 7, isBot, alreadySent);
												}
												
												break;
											case "qp":
												
												sendOK(out);
												printEntries(out, Integer.parseInt(a1Value), 7, isBot, new HashSet<>());
												
												break;
											case "p":

												sendOK(out);
												//ob_start();
												//printEntries($db, $id, 500, false);
												//$postData = ob_get_clean();
												//$data = file_get_contents('template.html');
												//$i = strpos($data, '<bot></bot>');
												//echo substr($data, 0, $i);
												out.write(template[0]);
												//echo '<data>'.$postData.'</data>';
												out.write("<data>".getBytes());
												printEntries(out, Integer.parseInt(a1Value), 2000, isBot, new HashSet<>());
												out.write("</data>".getBytes());
												//$j = strpos($data, 'main.js', $i);
												//echo substr($data, $i+11, $j-$i-11);
												out.write(template[1]);
												//echo 'main1.js';
												out.write("main1.js".getBytes());
												//echo substr($data, $j+7);
												out.write(template[2]);
												
												break;
											default:
												sendOK(out);
												showDefault(out, isBot);
												break;
											}
											
										} else {
											sendOK(out);
											showDefault(out, isBot);
										}
										
									} else {
										// file request
										switch(file){
										case "/main.js":
										case "/main1.js":
										case "/img/404.jpg":
										case "/img/icon.gif":
										case "/img/menu.png":
										case "/img/no.pp.png":
										case "/img/youtubeOff.png":
										case "/img/youtubeOn.png":
											
											sendHeader(out, file.endsWith(".png") || file.endsWith(".jpg") || file.endsWith(".gif") ? "image/jpeg" : file.endsWith(".js") ? "text/javascript" : "text/html");
											
											InputStream in2 = new BufferedInputStream(getClass().getResourceAsStream("/raw"+file));
											byte[] buffer = new byte[1024];
											while(true){
												int di = in2.read(buffer);
												if(di < 0) break;
												out.write(buffer, 0, di);
											}
											
											break;
											
										default:// 404
											
											System.out.println("not found: "+file);
											out.write(("HTTP/1.1 404 Not found!\r\n\r\n").getBytes());
											
											break;
										}
									}
									
									out.flush();
									client.close();
									
								} catch(IOException e){
									e.printStackTrace();
								}
							}
						}).start();
					}
					
					
				} catch (IOException e) {
					
					Main.frame.setTitle(e.getMessage());
					e.printStackTrace();
					
				} finally {
					if(socket != null){
						try {
							socket.close();
						} catch(IOException e2){
							
						}
					}
				}
				
			}
		}).start();
	}

	public void close() {
		try {
			socket.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void restart(int port){
		close();
		this.port = port;
		listenAlways();
		Main.frame.setTitle(Main.title);
	}
}
