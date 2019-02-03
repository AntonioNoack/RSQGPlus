package main;

public class Main {

	public static void main(String[] args){
		
		new Server(new Structure(), 8080).listenAlways();
		
	}
	
}
