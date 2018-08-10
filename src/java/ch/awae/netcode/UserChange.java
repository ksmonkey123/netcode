package ch.awae.netcode;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class UserChange implements Serializable {
	private static final long serialVersionUID = 1L;
	private String userId;
	private boolean joined;
}
