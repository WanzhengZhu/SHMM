package data;

import org.apache.commons.math3.linear.RealVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Checkin implements Serializable {

  int checkinId;
  long userId;
  Location location;
  Location real_location;
  int timestamp;
  int real_timestamp;
  Map<Integer, Integer> message;  // Key: word, Value:count
  RealVector messagevector;

  public Checkin(int checkInId, int timestamp, long userId, double lat, double lng, Map<Integer, Integer> message, RealVector messagevector, int info_option) {
    this.checkinId = checkInId;
    this.userId = userId;
    if ((info_option == 4) || (info_option == 5) || (info_option == 6) || (info_option == 8) || (info_option == 14) || (info_option == 15) || (info_option == 16) || (info_option == 18)) { // 4 is without time; 5 is with only text; 6 is with only location; 8 is with no information
      this.timestamp = 0;
    } else {
      this.timestamp = timestamp;
    }
    this.real_timestamp = timestamp;

    if ((info_option == 3) || (info_option == 5) || (info_option == 7) || (info_option == 8) || (info_option == 13) || (info_option == 15) || (info_option == 17) || (info_option == 18)) { // 3 is without location; 5 is with only text; 7 is with only time; 8 is with no information
      this.location = new Location(0, 0);
    } else {
      this.location = new Location(lng, lat);
    }
    this.real_location = new Location(lng, lat);  // For evaluation purpose only.
    this.message = message;
    this.messagevector = messagevector;
  }

  public int getId() {
    return checkinId;
  }

  public long getUserId() {
    return userId;
  }

  public Location getLocation() {
    return location;
  }

  public Location getreal_location() {
    return real_location;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public int getrealTimestamp() {
    return real_timestamp;
  }

  public Map<Integer, Integer> getMessage() {
    return message;
  }

  public RealVector getVector() {
    return messagevector;
  }

  public void setMessage(Map<Integer, Integer> message) {
	    this.message = message;
  }

  // Get the text of the message and the description of the location
  public String getText(WordDataset wd) {
    String s = "";
    for (Map.Entry<Integer, Integer> e : message.entrySet()) {
      int wid = e.getKey();
      s += wd.getWord(wid) + " ";
    }
    return s;
  }

  @Override
  public int hashCode() {
    return new Integer(checkinId).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Checkin))
      return false;
    if (obj == this)
      return true;
    return checkinId == ((Checkin) obj).getId();
  }

//  public Checkin copy(int info_option) {
//    Checkin res = new Checkin(checkinId, timestamp, userId, location.getLat(), location.getLng(), null, messagevector, info_option);
//    Map<Integer, Integer> copiedMessage = new HashMap(message);
//    res.setMessage(copiedMessage);
//    return res;
//  }

}
