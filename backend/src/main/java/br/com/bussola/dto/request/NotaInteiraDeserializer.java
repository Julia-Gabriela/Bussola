package br.com.bussola.dto.request;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/** Impede que o JSON converta silenciosamente 2.5 em 2 ou texto em nota. */
public final class NotaInteiraDeserializer extends StdScalarDeserializer<Integer> {
    public NotaInteiraDeserializer() { super(Integer.class); }

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
            return context.reportInputMismatch(Integer.class, "A nota deve ser um número inteiro de 1 a 5.");
        return parser.getIntValue();
    }
}
