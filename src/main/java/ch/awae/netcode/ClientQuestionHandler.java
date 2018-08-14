package ch.awae.netcode;

import java.io.Serializable;

/**
 * Handler for ClientQuestions (introduced in netcode 2.0.0).
 *
 * @since netcode 2.0.0
 * @author Andreas Wälchli
 */
@FunctionalInterface
public interface ClientQuestionHandler {

    Serializable handleQuestion(String from, Serializable data);

}