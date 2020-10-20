package app;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;

public class UserStorage {

    static HashMap<Integer, User> userMap = new HashMap<>();

    static {
        userMap.put(1, new User(1, "firstUser", "DE", 18));
        userMap.put(2, new User(2, "secondUser", "UK", 39));
        userMap.put(3, new User(3, "thirdUser", "UK", 43));
        userMap.put(4, new User(4, "fourthUser", "AT", 13));
        userMap.put(5, new User(5, "fifthUser", "AT", 16));
        userMap.put(6, new User(6, "fiveUser", "RU", 18));
        userMap.put(7, new User(7, "seventhUser", "RU", 28));
        userMap.put(8, new User(8, "eighthUser", "DE", 19));
        userMap.put(9, new User(9, "ninthUser", "FR", 31));
        userMap.put(10, new User(10, "tenthUser", "AT", 74));
    }

    public static JsonObject getUsernameById(int id) {
        if (!userMap.containsKey(id))
            throw new NoSuchUserException(id);

        JsonObject object = new JsonObject();
        String username = userMap.get(id).getUsername();
        object.put("username", username);
        return object;
    }

    public static JsonObject getCountryById(int id) {
        if (!userMap.containsKey(id))
            throw new NoSuchUserException(id);

        JsonObject object = new JsonObject();
        String country = userMap.get(id).getCountry();
        object.put("country", country);
        return object;
    }

    public static JsonObject getAge(int id){
        if (!userMap.containsKey(id))
            throw new NoSuchUserException(id);

        JsonObject object = new JsonObject();
        int age = userMap.get(id).getAge();
        object.put("age", age);
        return object;
    }
}
