package se.embargo.recall;

public class Phonenumbers {
	public static boolean isPrivateNumber(String phonenumber) {
		return phonenumber == null || "".equals(phonenumber);
	}
}
