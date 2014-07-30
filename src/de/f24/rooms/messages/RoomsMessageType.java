package de.f24.rooms.messages;

public enum RoomsMessageType {

	RegistrationRequest(101), Registration(102), RegistrationConfirmation(103), 
	PersonalInfo(201), ContactSync(204), ContactList(205), ContactSearch(206),  
	OpenRoomRequest(301), Invitation(302), RoomsConfiguration(303), 
	TextMessage(401), DeliveryReceipt(402), PushRequest(403),
	File(601);

	private int id;

	RoomsMessageType(int id) {
		this.id = id;
	}

	public int getValue() {
		return id;
	}
	
	public static RoomsMessageType ofType(int value) {
		for (RoomsMessageType type : RoomsMessageType.values()) {
			if (type.getValue() == value) {
				return type;
			}
		}
		return null;
	}
}
