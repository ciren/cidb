package net.cilib.cidb;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Stack;

public class Search {

    public static void search(Properties p, String query) {
        DB db = Util.connect(p).underlying().getDB(p.getProperty("db_database"));

        ShuntingYardParser parser = new ShuntingYardParser();
        Queue<String> searchString = parser.getSearchString(query);

        DBCollection tagCol = db.getCollection(p.getProperty("db_tagCol"));
        DBCollection simCol = db.getCollection(p.getProperty("db_simCol"));

        try {
            DBCursor cursor = simCol.find(buildQuery(searchString, tagCol));

            //TODO: return something useful
            while (cursor.hasNext()) {
                System.out.println(cursor.next().get("_id"));
            }
        } catch (Exception ex) {
            Util.exit("Failed to parse search query.", 1);
        }
    }

    private static ArrayList<String> getPossibleKeys(String input, boolean hasNot, DBCollection collection) {
        DBCursor cursor = collection.find(new BasicDBObject("term", input));
        ArrayList<String> list = new ArrayList<String>();

        while (cursor.hasNext()) {
            for (String key : (List<String>) cursor.next().get("keys")) {
                if (!list.contains(key)) {
                    if (!hasNot || (hasNot && key.contains(input + ".@type"))) {
                        list.add(key);
                    }
                }
            }
        }

        return list;
    }

    private static BasicDBObject buildQuery(Queue<String> stringQueue, DBCollection collection) {
        Stack<BasicDBObject> dbStack = new Stack<BasicDBObject>();

        for (String value : stringQueue) {
            if (value.contains(":")) {
                String[] split = value.split(":");
                String keyToSearch = (split[0].startsWith("!") ? split[0].substring(1) : split[0]);
                boolean hasNot = split[0].startsWith("!") ? true : false;

                ArrayList<String> keyList = getPossibleKeys(keyToSearch, hasNot, collection);

                if (keyList.size() > 1) {
                    ArrayList list = new ArrayList();
                    for (String key : keyList) {
                        BasicDBObject objectToAdd;
                        if (split[0].startsWith("!")) {
                            BasicDBObject innerObject = new BasicDBObject("$ne", parse(split[1]));
                            objectToAdd = new BasicDBObject(key, innerObject);
                        } else {
                            objectToAdd = new BasicDBObject(key, parse(split[1]));
                        }
                        list.add(objectToAdd);
                    }
                    if (hasNot) {
                        dbStack.push(new BasicDBObject("$and", list));
                    } else {
                        dbStack.push(new BasicDBObject("$or", list));
                    }
                } else if (keyList.size() == 1) {
                    BasicDBObject objectToAdd;
                    if (split[0].startsWith("!")) {
                        BasicDBObject innerObject = new BasicDBObject("$ne", parse(split[1]));
                        objectToAdd = new BasicDBObject(keyList.get(0), innerObject);
                    } else {
                        objectToAdd = new BasicDBObject(keyList.get(0), parse(split[1]));
                    }
                    dbStack.push(objectToAdd);
                } else {
                    BasicDBObject objectToAdd;
                    if (split[0].startsWith("!")) {
                        BasicDBObject innerObject = new BasicDBObject(keyToSearch, parse(split[1]));
                        objectToAdd = new BasicDBObject("$ne", innerObject);
                    } else {
                        objectToAdd = new BasicDBObject(keyToSearch, parse(split[1]));
                    }
                    dbStack.push(objectToAdd);
                }

            } else {
                if (value.equalsIgnoreCase("AND")) {
                    ArrayList andList = new ArrayList();
                    andList.add(dbStack.pop());
                    andList.add(dbStack.pop());
                    BasicDBObject and = new BasicDBObject("$and", andList);
                    dbStack.push(and);
                } else if (value.equalsIgnoreCase("OR")) {
                    ArrayList orList = new ArrayList();
                    orList.add(dbStack.pop());
                    orList.add(dbStack.pop());
                    BasicDBObject and = new BasicDBObject("$or", orList);
                    dbStack.push(and);
                }
            }
        }

        return dbStack.pop();
    }

    public static Object parse(String str) {
        double d;

        try {
            d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return str;
        }

        return d;
    }
}
