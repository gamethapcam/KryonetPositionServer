package positionserver;

import java.io.IOException;
import java.util.HashSet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import positionserver.Network.Login;
import positionserver.Network.Register;
import positionserver.Network.RegistrationRequired;
import positionserver.Network.RemoveCharacter;

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
                    
                    loggedIn(connection, character);
                    return;
                }
                
                if (object instanceof Register) {
                    
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
    
    protected void loggedIn(Connection connection, Character character) {
        // TODO Auto-generated method stub
        
    }

    protected Character loadCharacter(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main(String[] args) {
        Log.set(Log.LEVEL_DEBUG);
        new PositionServer();
    }
}
