package edu.cmu.oli.data;

import edu.cmu.oli.data.service.DataService;
import edu.cmu.oli.data.service.QuizDetailsForm;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import java.util.Map;

@ApplicationScoped
@RouteBase(path = "cloud")
public class Routes {

    @Inject
    DataService dataService;

    @Inject
    Logger log;

    @Route(path = "/quiz/data", methods = HttpMethod.POST)
    Uni<JsonObject> processQuizData(RoutingContext rc, @Body @Valid QuizDetailsForm formData) {
        log.info("Security header " +rc.request().headers().get("secure"));
        try {
            formData.validate();
        } catch (Exception e) {
            return Uni.createFrom().item(new JsonObject(Map.of("error", e.getLocalizedMessage())));
        }

        return dataService.quizData(formData);
    }
}
