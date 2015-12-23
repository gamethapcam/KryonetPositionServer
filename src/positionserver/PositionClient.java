package positionserver;

import java.io.IOException;
import java.util.HashMap;

import javax.swing.JOptionPane;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.esotericsoftware.minlog.Log;

import positionserver.Network.AddCharacter;
import positionserver.Network.Login;
import positionserver.Network.MoveCharacter;
import positionserver.Network.Register;
import positionserver.Network.RegistrationRequired;
import positionserver.Network.RemoveCharacter;
import positionserver.Network.UpdateCharacter;

public class PositionClient {

	Client client;
	UI ui;
	String name;
	
	public PositionClient() {
		client = new Client();
		client.start();
		
		Network.register(client);
		
		client.addListener(new ThreadedListener(new Listener() {

			@Override
			public void connected(Connection connection) {
			}

			@Override
			public void received(Connection connection, Object object) {
				if (object instanceof RegistrationRequired) {
					Register register = new Register();
					register.name = name;
					register.otherStuff = ui.inputOtherStuff();
					client.sendTCP(register);
					return;
				}
				
				if (object instanceof AddCharacter) {
					AddCharacter msg = (AddCharacter) object;
					ui.addCharacter(msg.character);
					return;
				}
				
				if (object instanceof UpdateCharacter) {
					UpdateCharacter msg = (UpdateCharacter) object;
					ui.updateCharacter(msg);
					return;
				}
				
				if (object instanceof RemoveCharacter) {
					RemoveCharacter msg = (RemoveCharacter) object;
					ui.removeCharacter(msg.id);
					return;
				}
			}
			
			@Override
			public void disconnected(Connection connection) {
				System.exit(0);
			}
			
		}));
		
		ui = new UI();
		
		String host = ui.inputHost();
		
		try {
			client.connect(500, host, Network.PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		name = ui.inputName();
		Login login = new Login();
		login.name = name;
		client.sendTCP(login);
		
		
		while (true) {
			int ch;
			
			try {
				ch = System.in.read();
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
			
			MoveCharacter msg = new MoveCharacter();
			switch(ch) {
				case 'w':
					msg.y = -1;
					break;
				case 's':
					msg.y = 1;
					break;
				case 'a':
					msg.x = -1;
					break;
				case 'd':
					msg.x = 1;
					break;
				case 'q':
					client.close();
					System.exit(0);
					break;
				default:
					msg = null;
					break;
			}
			
			if (msg != null) {
				client.sendTCP(msg);
			}
		}
	}
	
	
	class UI {
		HashMap<Integer, Character> characters = new HashMap<>();
		
		public String inputHost() {
			String host = (String) JOptionPane.showInputDialog(null, "Host:", "Connect to Server", JOptionPane.QUESTION_MESSAGE, null, null, "localhost");
			if (host == null || host.trim().length() == 0) {
				System.exit(1);
			}
			return host.trim();
		}
		
		public String inputName() {
			String name = (String) JOptionPane.showInputDialog(null, "Name:", "Connect to Server", JOptionPane.QUESTION_MESSAGE, null, null, "Player1");
			if (name == null || name.trim().length() == 0) {
				System.exit(1);
			}
			return name.trim();
		}
		
		public String inputOtherStuff() {
			String otherStuff = (String) JOptionPane.showInputDialog(null, "Other stuff:", "Connect to Server", JOptionPane.QUESTION_MESSAGE, null, null, "something");
			if (otherStuff == null || otherStuff.trim().length() == 0) {
				System.exit(1);
			}
			return otherStuff.trim();
		}
		
		public void addCharacter(Character character) {
			characters.put(character.id, character);
			System.out.println(character.name + " added at " + "(" + character.x + ", " + character.y + ")");
		}
		
		public void updateCharacter(UpdateCharacter msg) {
			Character character = characters.get(msg.id);
			if (character == null) {
				return;
			}
			character.x = msg.x;
			character.y = msg.y;
			System.out.println(character.name + " moved to " + "(" + character.x + ", " + character.y + ")");
		}
		
		public void removeCharacter(int id) {
			Character character = characters.remove(id);
			if (character != null) {
				System.out.println(character.name + " removed");
			}
		}
	}
	
	public static void main(String[] args) {
		Log.set(Log.LEVEL_NONE);
		new PositionClient();
	}
}
