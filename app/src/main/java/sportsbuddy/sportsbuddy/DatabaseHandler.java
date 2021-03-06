package sportsbuddy.sportsbuddy;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by s165700 on 3/19/2018.
 */

/**
 * Private variables to hold information about the databases.
 * Used for easier communication between server and local database.
 * Methods for database communication.
 * NOTE: All interaction with any of the databases should be done through this class to keep things consistent.
 * You should not call methods directly from SQLiteHelper class and should not create any more instances of it.
 */
public class DatabaseHandler {
    private static boolean isSetUp = false;
    private static DatabaseHandler databaseHandler = new DatabaseHandler();
    private static FirebaseDatabase database;
    private static FirebaseUser firebaseUser;
    private static DatabaseReference timeTableRef;
    private static DatabaseReference usersRef;
    private static SQLiteHelper sqLiteHelper;
    private static List<UserTimeTable> userTimeTable;


    //To make sure only one instance of the DatabaseHandler is created
    protected static DatabaseHandler getDatabaseHandler() {
        if (!isSetUp) {
            database = FirebaseDatabase.getInstance();
            firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            timeTableRef = database.getReference("TimeTableSlot");
            usersRef = database.getReference("UsersInfo");
            userTimeTable = new ArrayList<>();
            isSetUp = true;
        }
        return databaseHandler;
    }

    /*
     * <--- Helper methods START --->
     */

    /*
     * <--- Helper methods END --->
     */

    /*
     * <--- Database management methods START --->
     */

    /**
     * Adds a new Timeslot to the server database
     */
    public static void addNewTimeSlotToServerDatabase(String sport,String level, String day, String timeFrom, String timeTo) {
        //Create a timetable slot in the server database with a unique ID
        DatabaseReference newTimeTableSlot = timeTableRef.push().getRef();
        //Add the unique userID which is final and consistent with the database
        newTimeTableSlot.child("User").setValue(firebaseUser.getUid());
        //Add the rest of the slot information to an Event child with corresponding names
        newTimeTableSlot.child("Event").child("Activity").setValue(sport);
        newTimeTableSlot.child("Event").child("Day").setValue(day);
        newTimeTableSlot.child("Event").child("TimeFrom").setValue(timeFrom);
        newTimeTableSlot.child("Event").child("TimeTo").setValue(timeTo);
        newTimeTableSlot.child("Event").child("Level").setValue(level);
        //Insert into the local database
        sqLiteHelper.insertTimeTableSlotDetails(newTimeTableSlot.getKey(),level, sport, day, timeFrom, timeTo);
    }

    /**
     * Checks for matches of a specific type.
     * This method will only return a matches from a specific set of values. It will not return
     * ALL of the matches for all sports that the user has entered.
     * This must be done by calling this method with multiple different params.
     *
     * @param userTimeTable
     */
    public void checkForMatches(final ArrayList<UserTimeTable> userTimeTable, final MatchesTab matchesTab) {
        DatabaseReference reference = database.getReference("TimeTableSlot");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Match> matches = new ArrayList<Match>();
                for(UserTimeTable userTimeTable1 : userTimeTable) {
                    String activity = userTimeTable1.getActivity();
                    String day = userTimeTable1.getDay();
                    String timeFrom = userTimeTable1.getTimeFrom();
                    String timeTo = userTimeTable1.getTimeTo();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        //Get data for user to be compared
                        String userIDSecond = String.valueOf(snapshot.child("User").getValue());
                        String sportSecond = String.valueOf(snapshot.child("Event").child("Activity").getValue());
                        String daySecond = String.valueOf(snapshot.child("Event").child("Day").getValue());
                        String timeFromDB = String.valueOf(snapshot.child("Event").child("TimeFrom").getValue());
                        String timeToDB = String.valueOf(snapshot.child("Event").child("TimeTo").getValue());
                        String levelUser2 = String.valueOf(snapshot.child("Event").child("Level").getValue());
                        //Check if its a different user doing the same activity for the same day
                        if (!userIDSecond.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) &&
                                sportSecond.equals(activity) &&
                                daySecond.equals(day)) {
                            String levelUser1 = userTimeTable1.getLevel();
                            //Get the timing values
                            timeFromDB = String.valueOf(timeFromDB.charAt(0)) +
                                    String.valueOf(timeFromDB.charAt(1)) +
                                    String.valueOf(timeFromDB.charAt(3)) +
                                    String.valueOf(timeFromDB.charAt(4));
                            timeToDB = String.valueOf(timeToDB.charAt(0)) +
                                    String.valueOf(timeToDB.charAt(1)) +
                                    String.valueOf(timeToDB.charAt(3)) +
                                    String.valueOf(timeToDB.charAt(4));
                            String timeFromF = String.valueOf(timeFrom.charAt(0)) +
                                    String.valueOf(timeFrom.charAt(1)) +
                                    String.valueOf(timeFrom.charAt(3)) +
                                    String.valueOf(timeFrom.charAt(4));
                            String timeToF = String.valueOf(timeTo.charAt(0)) +
                                    String.valueOf(timeTo.charAt(1)) +
                                    String.valueOf(timeTo.charAt(3)) +
                                    String.valueOf(timeTo.charAt(4));
                            int timeFromDBInt = Integer.parseInt(timeFromDB);
                            int timeToDBInt = Integer.parseInt(timeToDB);
                            int timeFromInt = Integer.parseInt(timeFromF);
                            int timeToInt = Integer.parseInt(timeToF);
                            //Check if it is an overlapping timeslot
                            boolean isOverlapping = false;
                            String finalTimeFrom = "";
                            String finalTimeTo = "";
                            if(timeFromInt <= timeFromDBInt && timeToInt >= timeToDBInt){
                                //Case when the current user's timeslot is completely covering the databases timeslot
                                isOverlapping = true;
                                finalTimeFrom = String.valueOf(timeFromDBInt);
                                finalTimeTo = String.valueOf(timeToDBInt);

                            } else
                            if(timeFromDBInt <= timeFromInt && timeToDBInt >= timeToInt){
                                //case when the current user's timeslot is being completely overlapped by the database timeslot
                                isOverlapping = true;
                                finalTimeFrom = String.valueOf(timeFromInt);
                                finalTimeTo = String.valueOf(timeToInt);

                            } else
                            if(timeFromInt <= timeFromDBInt && timeFromInt< timeToDBInt && timeToInt<= timeToDBInt){
                                //case when the current user's timeslot is overlapping with the dabase timeslot but the current users time slot is to the right of the database timeslot
                                isOverlapping = true;
                                finalTimeFrom = String.valueOf(timeFromDBInt);
                                finalTimeTo = String.valueOf(timeToInt);
                            } else
                            if(timeFromDBInt <= timeFromInt && timeFromDBInt < timeToDBInt && timeToDBInt <= timeToInt ){
                                //case when the current user's timeslot is overlapping with the dabase timeslot but the current users time slot is to the left of the database timeslot
                                isOverlapping = true;
                                finalTimeFrom = String.valueOf(timeFromInt);
                                finalTimeTo = String.valueOf(timeToDBInt);
                            }
                            if(isOverlapping){
                                boolean isTheSame = false;
                                Match newMatch = new Match(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                        userIDSecond,
                                        levelUser1,
                                        levelUser2,
                                        sportSecond,
                                        daySecond,
                                        finalTimeFrom,
                                        finalTimeTo,
                                        false);
                                for(Match match : matches){
                                    if(newMatch.getMatchUser2().equals(match.getMatchUser2()) &&
                                            newMatch.getSportingActivity().equals(match.getSportingActivity()) &&
                                            newMatch.getDay().equals(match.getDay()) &&
                                            newMatch.getTimeFromOverlap().equals(match.getTimeFromOverlap()) &&
                                            newMatch.getTimeToOverlap().equals(match.getTimeToOverlap())){
                                        isTheSame = true;
                                    }
                                }
                                if(!isTheSame) {
                                    matches.add(newMatch);
                                }
                            }
                        }
                    }
                }
                getOldMatches(matches, matchesTab);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getOldMatches(final ArrayList<Match> newMatches, final MatchesTab matchesTab){
        DatabaseReference reference = database.getReference("Matches");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                ArrayList<Match> activeMatches = new ArrayList<Match>();
                for(DataSnapshot data : dataSnapshot.getChildren()){

                    String sport = String.valueOf(data.child("Sport").getValue());
                    String day = String.valueOf(data.child("Day").getValue());
                    String timeFrom = String.valueOf(data.child("TimeFrom").getValue());
                    String timeTo = String.valueOf(data.child("TimeTo").getValue());
                    String handled = String.valueOf(data.child("Handled").getValue());
                    if(String.valueOf(data.child("UID1").getValue()).equals(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            ||String.valueOf(data.child("UID2").getValue()).equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        String matchUser1 = String.valueOf(data.child("UID1").getValue());
                        String matchUser2 = String.valueOf(data.child("UID2").getValue());
                        String levelUser1 = String.valueOf(data.child("LevelUser1").getValue());
                        String levelUser2 = String.valueOf(data.child("LevelUser2").getValue());
                        if(handled.equals("false")){
                            activeMatches.add(new Match(matchUser1, matchUser2,levelUser1,levelUser2,sport,day,timeFrom,timeTo,false));
                        }else {
                            activeMatches.add(new Match(matchUser1, matchUser2,levelUser1,levelUser2,sport,day,timeFrom,timeTo,true));
                        }
                    }
                }
                compareMatches(newMatches,activeMatches, matchesTab);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void compareMatches(ArrayList<Match> newMatches, ArrayList<Match> oldMatches, MatchesTab matchesTab){
        ArrayList<Match> finalMatches = new ArrayList<Match>();
        ArrayList<Match> matchesToBeAdded = new ArrayList<Match>();

        boolean isTheSame;
        if(oldMatches.isEmpty()){
            finalMatches = newMatches;
            matchesToBeAdded = newMatches;
        } else {
            for(Match match : oldMatches){
                finalMatches.add(match);
            }
            for(Match newMatch : newMatches){
                isTheSame = false;
                for(Match oldMatch : oldMatches){
                    if(((newMatch.getMatchUser1().equals(oldMatch.getMatchUser1()) &&
                            newMatch.getMatchUser2().equals(oldMatch.getMatchUser2())) ||
                            (newMatch.getMatchUser2().equals(oldMatch.getMatchUser1()) &&
                            newMatch.getMatchUser1().equals(oldMatch.getMatchUser2()))) &&
                            newMatch.getSportingActivity().equals(oldMatch.getSportingActivity()) &&
                            newMatch.getDay().equals(oldMatch.getDay()) &&
                            newMatch.getTimeFromOverlap().equals(oldMatch.getTimeFromOverlap()) &&
                            newMatch.getTimeToOverlap().equals(oldMatch.getTimeToOverlap())){
                        newMatch.setHandled(oldMatch.isHandled());
                        isTheSame = true;
                    }
                    /* else {
                        // Used for debugging DONT DELETE
                        Log.e("They are", "different");
                        Log.e("Now Showing","New Match");
                        Log.e("Sport", newMatch.getSportingActivity());
                        Log.e("Day", newMatch.getDay());
                        Log.e("UID1", newMatch.getMatchUser1());
                        Log.e("UID2", newMatch.getMatchUser2());
                        Log.e("From", newMatch.getTimeFromOverlap());
                        Log.e("To", newMatch.getTimeToOverlap());
                        Log.e("Now Showing","Old Match");
                        Log.e("Sport", oldMatch.getSportingActivity());
                        Log.e("Day", oldMatch.getDay());
                        Log.e("UID1", oldMatch.getMatchUser1());
                        Log.e("UID2", oldMatch.getMatchUser2());
                        Log.e("From", oldMatch.getTimeFromOverlap());
                        Log.e("To", oldMatch.getTimeToOverlap());
                        if(!matchesToBeAdded.contains(newMatch) && !finalMatches.contains(newMatch)){
                            // Log.e("Adding them", "NOW");
                            finalMatches.add(newMatch);
                        }
                    }*/
                }
                if(!isTheSame){
                    finalMatches.add(newMatch);
                }
            }
        }
        for(Match match : finalMatches){
            if(!matchesToBeAdded.contains(match) && !oldMatches.contains(match)){
                // Log.e("Adding them", "NOW");
                matchesToBeAdded.add(match);
            }
        }
        ArrayList<Match> matchesToSend = new ArrayList<Match>();
        for(Match match : finalMatches){
            if(!match.isHandled()){
                matchesToSend.add(match);
            }
        }
        addMatchesToDatabase(matchesToBeAdded);
        getMatchUsers(matchesToSend,matchesTab);
    }

    private void addMatchesToDatabase(ArrayList<Match> matchesToBeAdded){
        for(Match match : matchesToBeAdded){
            DatabaseReference ref = database.getReference("Matches").push();
            ref.child("UID1").setValue(match.getMatchUser1());
            ref.child("UID2").setValue(match.getMatchUser2());
            ref.child("LevelUser1").setValue(match.getLevelUser1());
            ref.child("LevelUser2").setValue(match.getLevelUser2());
            ref.child("Day").setValue(match.getDay());
            ref.child("Sport").setValue(match.getSportingActivity());
            ref.child("TimeFrom").setValue(match.getTimeFromOverlap());
            ref.child("TimeTo").setValue(match.getTimeToOverlap());
            ref.child("Handled").setValue("false");
        }
    }

    public void setMatchHandled(final Match match){
        DatabaseReference ref = database.getReference("Matches");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    if(match.getMatchUser1().equals(String.valueOf(data.child("UID1").getValue()))&&
                            match.getMatchUser2().equals(String.valueOf(data.child("UID2").getValue())) &&
                            match.getDay().equals(String.valueOf(data.child("Day").getValue())) &&
                            match.getSportingActivity().equals(String.valueOf(data.child("Sport").getValue())) &&
                            match.getTimeFromOverlap().equals(String.valueOf(data.child("TimeFrom").getValue())) &&
                            match.getTimeToOverlap().equals(String.valueOf(data.child("TimeTo").getValue()))){
                        DatabaseReference ref = database.getReference("Matches").child(data.getKey()).child("Handled");
                        ref.setValue("true");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Upadates the server and local database with the new user info
    public static void updateUserInfo(String name, String gender, String age, String about) {
        usersRef.child(firebaseUser.getUid()).child("Name").setValue(name);
        usersRef.child(firebaseUser.getUid()).child("Gender").setValue(gender);
        usersRef.child(firebaseUser.getUid()).child("Age").setValue(age);
        usersRef.child(firebaseUser.getUid()).child("About").setValue(about);
        sqLiteHelper.updatePersonalProfileData(FirebaseAuth.getInstance().getUid(), name, age, gender, about);
    }

    //Upadates the server and local database with the new user info
    public static void updateProfilePicture(String profilePicture) {
        usersRef.child(firebaseUser.getUid()).child("ProfilePicture").setValue(profilePicture);
        sqLiteHelper.updateProfilePicture(FirebaseAuth.getInstance().getUid(), profilePicture);
    }

    //Gets a user from the online database. Not used ATM
    //TODO: Adapt and use this when viewing other profiles
    public void getUserInfoFromServer(final String uID, final ViewProfileActivity activity) {
        DatabaseReference ref = usersRef;

        ref.child(uID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                AppUser user  = new AppUser(FirebaseAuth.getInstance().getCurrentUser().getUid(),null,null,null,null,null);
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    user.setUID(uID);
                    if (snapshot.getKey().equals("Name")) {
                        user.setName(String.valueOf(snapshot.getValue()));
                    }
                    if (snapshot.getKey().equals("Age")) {
                        user.setAge(String.valueOf(snapshot.getValue()));
                    }
                    if (snapshot.getKey().equals("Gender")) {
                        user.setGender(String.valueOf(snapshot.getValue()));
                    }
                    if (snapshot.getKey().equals("About")) {
                        user.setAbout(String.valueOf(snapshot.getValue()));
                    }

                    activity.updateUserInfo(user);
                }
                usersRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    //gets user time table from database.
    public static void getUserTimeTableFromServer(final Callback<List<UserTimeTable>> callback) {
        timeTableRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //creates an empty list
                userTimeTable = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    if (Objects.equals(snapshot.child("User").getValue(), firebaseUser.getUid())) {
                        String key = snapshot.getKey();
                        String activity = String.valueOf(snapshot.child("Event").child("Activity").getValue());
                        String day = String.valueOf(snapshot.child("Event").child("Day").getValue());
                        String timeFrom = String.valueOf(snapshot.child("Event").child("TimeFrom").getValue());
                        String timeTo = String.valueOf(snapshot.child("Event").child("TimeTo").getValue());
                        String level = String.valueOf(snapshot.child("Event").child("Level").getValue());

                        UserTimeTable timeSlot = new UserTimeTable(key, level, activity, day, timeFrom, timeTo);

                        userTimeTable.add(timeSlot);
                    }
                }
                callback.call(userTimeTable);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

    }
    //removes timeslot in user timetable

    public static  void removeTimeSlot(String key){
        timeTableRef.child(key).removeValue();
        sqLiteHelper.deleteTimeTableSlot(key);
    }

    //returns an instance of the user information filled with data from the local database
    public static AppUser getUserInfoFromLocal() {
        String name = "blank";
        String age = "blank";
        String gender = "blank";
        String about = "blank";
        String profilePicture = null;
        Cursor cursor = sqLiteHelper.getData("SELECT COUNT(*) FROM Profile");
        boolean empty = true;
        if (cursor != null && cursor.moveToFirst()) {
            empty = (cursor.getInt(0) == 0);
        }
        cursor.close();
        Log.d("Reached", String.valueOf(empty));
        if (empty) {
            sqLiteHelper.insertPersonalProfileInfo(FirebaseAuth.getInstance().getUid(), "Blank", "0", "Blank", "Blank", "");
        } else {
            cursor = sqLiteHelper.getData("SELECT * FROM Profile");
            while (cursor.moveToNext()) {
                name = cursor.getString(2);
                age = cursor.getString(3);
                gender = cursor.getString(4);
                about = cursor.getString(5);
                profilePicture = cursor.getString(6);
            }
        }
        AppUser appUser = new AppUser(FirebaseAuth.getInstance().getUid(), name, age, gender, about, profilePicture);
        return appUser;
    }

    /**
     * This method is used to get the friends id's from the database in the FriendsActivity
     * After the static method setFriendsList in FriendsActivity is called
     * @param userList
     */
    public void getFriendsListIDS(final ArrayList<String> userList, final FriendsActivity activity){
        DatabaseReference friendsRef = database.getReference("FriendsLists").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        friendsRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot datum : dataSnapshot.getChildren()){
                    if(!userList.contains(String.valueOf(datum.getValue()))){
                        userList.add(String.valueOf(datum.getValue()));
                    }
                }
                activity.setFriendsListIDS(userList);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * This method updates the Friends list containing of AppUsers from the Database, comparing it to the ArrayList of string of users
     * @param userListIDS
     * @param userList
     * @param activity
     */
    public void getFriendsListUsers(final ArrayList<String> userListIDS, final ArrayList<AppUser> userList, final  FriendsActivity activity){
        DatabaseReference usersRef = database.getReference("UsersInfo");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot datum : dataSnapshot.getChildren()){
                    AppUser user = new AppUser(dataSnapshot.getKey(),null, null,null,null,null);
                    //If the user's id is in the friends list
                    if(userListIDS.contains(datum.getKey())){
                        user.setName(String.valueOf(datum.child("Name").getValue()));
                        user.setAge(String.valueOf(datum.child("Age").getValue()));
                        user.setAbout(String.valueOf(datum.child("About").getValue()));
                        user.setGender(String.valueOf(datum.child("Gender").getValue()));
                        user.setprofilePicture(String.valueOf(datum.child("ProfilePicture").getValue()));
                        user.setUID(datum.getKey());
                        if(!userList.contains(user)){
                            userList.add(user);
                        }

                    }
                }
                activity.setFriendsList(userList);


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /**
     * Adds a friend to the users friend list. UID is the new friend's UID
     * @param UID
     */
    public void AddToFriends(String UID){
        DatabaseReference friendsRef = database.getReference("FriendsList")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        friendsRef.child(UID).setValue("UID");
    }

    /**
     * Remove a friend with UID from the user's database.
     * @param UID
     */
    public void RemoveFromFriends(String UID){
        DatabaseReference friendsRef = database.getReference("FriendsList")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        friendsRef.child(UID).removeValue();
    }



    public void sendMatchRequest(Match match){
        DatabaseReference ref = database.getReference("Requests").push();
        ref.child("RequestFrom").setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
        if(match.getMatchUser1().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
            ref.child("RequestTo").setValue(match.getMatchUser2());
        } else {
            ref.child("RequestTo").setValue(match.getMatchUser1());
        }
        ref.child("Sport").setValue(match.getSportingActivity());
        ref.child("TimeFrom").setValue(match.getTimeFromOverlap());
        ref.child("TimeTo").setValue(match.getTimeToOverlap());
        ref.child("Handled").setValue("false");
    }

    public ArrayList<UserTimeTable> getSlotsFromLocal(){
        ArrayList<UserTimeTable> userTimeTableArray = new ArrayList<>();
        Cursor c = sqLiteHelper.getData("SELECT * FROM Slots");
        String key;
        String level;
        String activity;
        String day;
        String timeFrom;
        String timeTo;

        while(c.moveToNext()){
            key = c.getColumnName(1);
            level = c.getString(2);
            activity = c.getString(3);
            day = c.getString(4);
            timeFrom = c.getString(5);
            timeTo = c.getString(6);
            userTimeTableArray.add(new UserTimeTable(key,level,activity,day,timeFrom,timeTo));
        }
        return userTimeTableArray;
    }

    public void getMatchUsers(final ArrayList<Match> matches, final MatchesTab matchesTab){
        Log.e("Size of matches is:", String.valueOf(matches.size()));
        DatabaseReference ref = database.getReference("UsersInfo");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<AppUser> userToReturn = new ArrayList<>();
                for(Match match : matches){
                    Log.e("Checking1", match.getMatchUser1());
                    Log.e("Checking2", match.getMatchUser2());
                    boolean isFound = false;
                    for(DataSnapshot data : dataSnapshot.getChildren()){
                        //Who is the current user
                        AppUser appUser = new AppUser(
                                String.valueOf(data.getKey()),
                                String.valueOf(data.child("Name").getValue()),
                                String.valueOf(data.child("Age").getValue()),
                                String.valueOf(data.child("Gender").getValue()),
                                String.valueOf(data.child("About").getValue()),
                                String.valueOf(data.child("ProfilePicture").getValue())
                        );
                        if(match.getMatchUser2().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                            //In this case we are looking for an entry with matchUser1
                            if(match.getMatchUser1().equals(String.valueOf(data.getKey()))){
                                userToReturn.add(appUser);
                                Log.e("Added1", appUser.getName());
                                isFound = true;
                            }
                        } else {
                            if(match.getMatchUser2().equals(String.valueOf(data.getKey()))){
                                userToReturn.add(appUser);
                                Log.e("Added2", appUser.getName());
                                isFound = true;
                            }
                        }
                    }
                    if(!isFound){
                        userToReturn.add(new AppUser("","","","","",""));
                    }
                }
                matchesTab.updateMatches(userToReturn,matches);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void getRequests(final RequestsTab requestsTab){
        DatabaseReference ref = database.getReference("Requests");
        final ArrayList<Request> requests = new ArrayList<Request>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    if(String.valueOf(data.child("RequestTo").getValue()).equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) &&
                            String.valueOf(data.child("Handled").getValue()).equals("false")){
                        String UID = String.valueOf(data.child("RequestFrom").getValue());
                        String sport = String.valueOf(data.child("Sport").getValue());
                        String timeFrom = String.valueOf(data.child("TimeFrom").getValue());
                        String timeTo = String.valueOf(data.child("TimeTo").getValue());
                        String day = String.valueOf(data.child("Day").getValue());
                        requests.add(new Request(UID, sport,day, timeFrom, timeTo,"blank",false));
                    }
                }
                getRequestsUsers(requests, requestsTab);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getRequestsUsers(final ArrayList<Request> requests, final RequestsTab requestsTab){
        final ArrayList<String> userIDs = new ArrayList<String>();
        final ArrayList<AppUser> appUsers = new ArrayList<AppUser>();
        for(Request request : requests){
            userIDs.add(request.getUID());
        }

        DatabaseReference ref = database.getReference("UsersInfo");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(String userId : userIDs){
                    for(DataSnapshot data : dataSnapshot.getChildren()){
                        AppUser appUser = new AppUser(
                                String.valueOf(data.getKey()),
                                String.valueOf(data.child("Name").getValue()),
                                String.valueOf(data.child("Age").getValue()),
                                String.valueOf(data.child("Gender").getValue()),
                                String.valueOf(data.child("About").getValue()),
                                String.valueOf(data.child("ProfilePicture").getValue())
                        );
                        if(userId.equals(String.valueOf(data.getKey()))){
                            Log.e("Added", data.getKey());
                            appUsers.add(appUser);
                        }
                    }
                }
                getRequestedUserLevels(requests,appUsers,requestsTab);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void getRequestedUserLevels(final ArrayList<Request> requests, final ArrayList<AppUser> users, final RequestsTab requestsTab){
        final DatabaseReference reference = database.getReference("TimeTableSlot");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data: dataSnapshot.getChildren()){
                    for(AppUser user : users) {
                        if (String.valueOf(data.child("User").getValue()).equals(user.getUID())){
                            //Check if the request is aimed at the currentUser
                            for(Request request : requests) {
                                if (String.valueOf(data.child("Event").child("Activity").getValue()).equals(request.getSportingActivity())) {
                                        request.setLevel(String.valueOf(data.child("Event").child("Level").getValue()));
                                }
                            }
                        }
                    }
                }
                requestsTab.setRequests(requests, users);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void addToFriends(String UID){
        DatabaseReference myRef = database.getReference("FriendsLists").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).push();
        DatabaseReference friendsRef = database.getReference("FriendsLists").child(UID).push();
        myRef.setValue(UID);
        friendsRef.setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    public void setRequestHandled(final AppUser appUser, final Request request, Boolean isAccepted){
        final DatabaseReference reference = database.getReference("Requests");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    if(String.valueOf(data.child("RequestTo").getValue()).equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) &&
                            String.valueOf(data.child("RequestFrom").getValue()).equals(appUser.getUID()) &&
                            String.valueOf(data.child("Sport").getValue()).equals(request.getSportingActivity()) &&
                            String.valueOf(data.child("TimeFrom").getValue()).equals(request.getTimeFromOverlap()) &&
                            String.valueOf(data.child("TimeTo").getValue()).equals(request.getTimeToOverlap())){
                        DatabaseReference databaseReference = database.getReference("Requests").child(data.getKey()).child("Handled");
                        databaseReference.setValue("true");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void createConversation(final AppUser appUser, final MessagingActivity messagingActivity){
        final DatabaseReference ref = database.getReference("Messages");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean exists = false;
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    if((String.valueOf(snapshot.child("Users").child("User1").getValue()).equals(appUser.getUID()) &&
                            String.valueOf(snapshot.child("Users").child("User2").getValue()).equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) ||
                            (String.valueOf(snapshot.child("Users").child("User2").getValue()).equals(appUser.getUID()) &&
                                    String.valueOf(snapshot.child("Users").child("User1").getValue()).equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))){
                        exists = true;
                        messagingActivity.setConversationID(snapshot.getKey());
                    }
                }
                if(!exists){
                    DatabaseReference reference = ref.push();
                    reference.child("Users").child("User1").setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    reference.child("Users").child("User2").setValue(appUser.getUID());
                    messagingActivity.setConversationID(reference.getKey());
                }
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getMessages(String conversationID, AppUser appUser, final MessagingActivity activity){


        DatabaseReference ref = database.getReference("Messages").child(conversationID).child("Messages");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Message> messages = new ArrayList<Message>();
                for(DataSnapshot message : dataSnapshot.getChildren()){
                    String content = String.valueOf(message.child("Content").getValue());
                    String sender = String.valueOf(message.child("Sender").getValue());
                    String timeSent = String.valueOf(message.child("TimeSent").getValue());
                    //check if the sender is the current user
                    messages.add(new Message(content,sender,timeSent));
                }
                activity.setMessages(messages);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void sendMessage(final Message message, String conversationID, AppUser appUser, MessagingActivity messagingActivity){
        DatabaseReference ref = database.getReference("Messages").child(conversationID).child("Messages").push();
        ref.child("Sender").setValue(message.getSender());
        ref.child("Content").setValue(message.getMsgContent());
        ref.child("TimeSend").setValue(message.getTimeSent());
    }

    public void getMyPicture(final MessagingActivity messagingActivity){
        DatabaseReference ref = database.getReference("UsersInfo").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String profilePic = String.valueOf(dataSnapshot.child("ProfilePicture").getValue());
                messagingActivity.setMyPic(profilePic);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /*
     * <--- Database management methods END --->
     */

    /*
     * <--- Getters and setters START --->
     */

    //sets up the local database.
    public static void setSqLiteHelper(SQLiteHelper _sqLiteHelper) {
        sqLiteHelper = _sqLiteHelper;
        //TODO: Uncomment the two lines below, delete your app and install it again if it crashes. Then comment this line back.
        //sqLiteHelper.queryData("DROP TABLE IF EXISTS Slots");
        //sqLiteHelper.queryData("DROP TABLE Profile");
        sqLiteHelper.queryData
                ("CREATE TABLE IF NOT EXISTS Slots(Id INTEGER PRIMARY KEY AUTOINCREMENT, slotID VARCHAR, level VARCHAR, activity VARCHAR, day VARCHAR, timeFrom VARCHAR, timeTo VARCHAR)");
        sqLiteHelper.queryData
                ("CREATE TABLE IF NOT EXISTS Profile(Id INTEGER PRIMARY KEY AUTOINCREMENT, uID VARCHAR, name VARCHAR, age VARCHAR, gender VARCHAR, about VARCHAR, profilePicture VARCHAR)");
    }

    public static FirebaseDatabase getDatabase() {
        return database;
    }

    public static void setDatabase(FirebaseDatabase database) {
        DatabaseHandler.database = database;
    }

    public static DatabaseReference getTimeTableRef() {
        return timeTableRef;
    }

    public static void setTimeTableRef(DatabaseReference timeTableRef) {
        DatabaseHandler.timeTableRef = timeTableRef;
    }

    public static FirebaseUser getFirebaseUser() {
        return firebaseUser;
    }

    public static void setFirebaseUser(FirebaseUser firebaseUser) {
        DatabaseHandler.firebaseUser = firebaseUser;
    }

    public static List<UserTimeTable> getUserTimeTable() {
        return userTimeTable;
    }
    /*
     * <--- Gertters and setters END --->
     */
}
