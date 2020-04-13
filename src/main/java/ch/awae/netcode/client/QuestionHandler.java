package ch.awae.netcode.client;

import java.io.Serializable;
import java.sql.Timestamp;

@FunctionalInterface
public interface QuestionHandler {

    Serializable handleQuestion(String sender, Timestamp timestamp, Serializable question);

}
