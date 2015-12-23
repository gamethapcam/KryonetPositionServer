package positionserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import positionserver.Network.AddCharacter;
import positionserver.Network.Login;
import positionserver.Network.MoveCharacter;
import positionserver.Network.Register;
import positionserver.Network.RegistrationRequired;
import positionserver.Network.RemoveCharacter;
import positionserver.Network.UpdateCharacter;

public class PositionServer {
    
    Server server;
    HashSet<Character> loggedIn = new HashSet<>();
    
    public PositionServer() {
        server = new Server() {
            @Override
            protected Connection newConnection() {
                return new CharacterConnection();
            }
            
        };
        
        Network.register(server);
        server.addListener(new Listener() {

            @Override
            public void received(Connection connection, Object object) {
                CharacterConnection c = (CharacterConnection) connection;
                Character character = c.character;
                
                if (object instanceof Login) {
                    if (character != null) {
                        return;
                    }
                    
                    String name = ((Login) object).name;
                    if (!isValid(name)) {
                        c.close();
                        return;
                    }
                    
                    for (Character ch : loggedIn) {
                        if (ch.name.equals(name)) {
                            c.close();
                            return;
                        }
                    }
                    
                    character = loadCharacter(name);
                    
                    if (character == null) {
                        c.sendTCP(new RegistrationRequired());
                        return;
                    }
                    
                    loggedIn(c, character);
                    return;
                }
                
                if (object instanceof Register) {
                    if (character != null) {
                    	return;
                    }
                    
                    Register register = (Register) object;
                    
                    if (!isValid(register.name)) {
                    	c.close();
                    	return;
                    }
                    
                    if (!isValid(register.otherStuff)) {
                    	c.close();
                    	return;
                    }
                    
                    character = new Character();
                    character.name = register.name;
                    character.otherStuff = register.otherStuff;
                    character.x = 0;
                    character.y = 0;
                    
                    if (!saveCharacter(character)) {
                    	c.close();
                    	return;
                    }
                    
                    loggedIn(c, character);
                    return;
                }
                
                if (object instanceof MoveCharacter) {
                	if (character == null) {
                		return;
                	}
                	
                	MoveCharacter msg = (MoveCharacter) object;
                	
                	// ignore invalid move
                	if ((Math.abs(msg.x) != 1) && (Math.abs(msg.y) != 1)) {
                		return;
                	}
                	
                	character.x += msg.x;
                	character.y += msg.y;
                	
                	if (!saveCharacter(character)) {
                		c.close();
                		return;
                	}

                	UpdateCharacter updateCharacter = new UpdateCharacter();
                	updateCharacter.id = character.id;
                	updateCharacter.x = character.x;
                	updateCharacter.y = character.y;
                	
                	server.sendToAllTCP(updateCharacter);
                	return;
                }
            }
            
            private boolean isValid(String value) {
                if (value == null) {
                    return false;
                }
                value = value.trim();
                if (value.length() == 0) {
                    return false;
                }
                return true;
            }
            
            @Override
            public void disconnected(Connection connection) {
                CharacterConnection c = (CharacterConnection) connection;
                if (c.character != null) {
                    loggedIn.remove(c.character);
                    
                    RemoveCharacter removeCharacter = new RemoveCharacter();
                    removeCharacter.id = c.character.id;
                    server.sendToAllTCP(removeCharacter);
                }
                
            }
        });
        
        server.start();
        try {
            server.bind(Network.PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void loggedIn(CharacterConnection connection, Character character) {
    	connection.character = character;
    	for (Character other : loggedIn) {
    		AddCharacter addCharacter = new AddCharacter();
    		addCharacter.character = other;
    		connection.sendTCP(addCharacter);
    	}
    	
    	loggedIn.add(character);
    	
    	AddCharacter addCharacter = new AddCharacter();
    	addCharacter.character = character;
    	server.sendToAllTCP(addCharacter);
    }

    protected boolean saveCharacter(Character character) {
    	File file = new File("characters", character.name.toLowerCase());
    	file.getParentFile().mkdirs();
    	
    	if (character.id == 0) {
    		String[] children = file.getParentFile().list();
    		if (children == null) {
    			return false;
    		}
    		
    		character.id = children.length + 1;
    	}
    	
    	DataOutputStream output = null;
    	
    	try {
			output = new DataOutputStream(new FileOutputStream(file));
			output.writeInt(character.id);
			output.writeUTF(character.otherStuff);
			output.writeInt(character.x);
			output.writeInt(character.y);
			
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
    	finally {
    		try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }

    protected Character loadCharacter(String name) {
    	File file = new File("characters", name.toLowerCase());
    	if (!file.exists()) {
    		return null;
    	}
    	
    	DataInputStream input = null;
    	
    	try {
			input = new DataInputStream(new FileInputStream(file));
			Character character = new Character();
			character.id = input.readInt();
			character.name = name;
			character.otherStuff = input.readUTF();
			character.x = input.readInt();
			character.y = input.readInt();
			input.close();
			return character;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    	finally {
    		if (input != null) {
    			try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    	
    }

    public static void main(String[] args) {
        Log.set(Log.LEVEL_NONE);
        new PositionServer();
    }
}
