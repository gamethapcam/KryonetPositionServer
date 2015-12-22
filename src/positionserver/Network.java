package positionserver;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {
    public static final int PORT = 56789;
    
    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(Login.class);
        kryo.register(RegistrationRequired.class);
        kryo.register(Register.class);
        kryo.register(UpdateCharacter.class);
        kryo.register(AddCharacter.class);
        kryo.register(RemoveCharacter.class);
        kryo.register(MoveCharacter.class);
    }
    
    public static class Login {
        public String name;
    }
    
    public static class RegistrationRequired {
        
    }
    
    public static class Register {
        public String name;
        public String otherStuff;
    }
    
    public static class UpdateCharacter {
        public int id;
        public int x, y;
    }
    
    public static class AddCharacter {
        public Character character;
    }
    
    public static class RemoveCharacter {
        public int id;
    }
    
    public static class MoveCharacter {
        public int x, y;
    }
}
