package study.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import study.api.core.product.Product;
import study.api.core.product.ProductService;
import study.api.core.recommendation.Recommendation;
import study.api.core.recommendation.RecommendationService;
import study.api.core.review.Review;
import study.api.core.review.ReviewService;
import study.api.event.Event;
import study.api.event.Event.Type;
import study.api.exceptions.InvalidInputException;
import study.api.exceptions.NotFoundException;
import study.util.http.HttpErrorInfo;

import java.io.IOException;

import static java.util.logging.Level.FINE;
import static reactor.core.publisher.Flux.empty;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService,
    ReviewService {
  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);
  private static final String PRODUCT_SERVICE_URL = "http://product";
  private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
  private static final String REVIEW_SERVICE_URL = "http://review";
  private final WebClient webClient;
  private final ObjectMapper mapper;
  private final StreamBridge streamBridge;
  private final Scheduler publishEventScheduler;

  public ProductCompositeIntegration(
      @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
      WebClient.Builder webClientBuilder,
      ObjectMapper mapper,
      StreamBridge streamBridge) {

    this.publishEventScheduler = publishEventScheduler;
    this.webClient = webClientBuilder.build();
    this.mapper = mapper;
    this.streamBridge = streamBridge;
  }

  @Override
  public Mono<Product> getProduct(int productId) {
    String url = PRODUCT_SERVICE_URL + "/product/" + productId;
    LOG.debug("Will call the getProduct API on URL: {}", url);

    return webClient.get().uri(url)
        .retrieve()
        .bodyToMono(Product.class)
        .log(LOG.getName(), Level.FINE)
        .onErrorMap(WebClientResponseException.class, this::handleException);
  }

  @Override
  public Mono<Product> createProduct(Product body) {
    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event<>(Event.Type.CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    return Mono.fromRunnable(()
            -> sendMessage("products-out-0", new Event<>(Event.Type.DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    String url = RECOMMENDATION_SERVICE_URL + "/recommendation?productId=" + productId;

    LOG.debug("Will call the getRecommendations API on URL: {}", url);

    return webClient.get().uri(url)
        .retrieve()
        .bodyToFlux(Recommendation.class)
        .log(LOG.getName(), Level.FINE)
        .onErrorResume(e -> empty());
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {
    return Mono.fromCallable(() -> {
      sendMessage("recommendations-out-0", new Event<>(Type.CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    return Mono.fromRunnable(() ->
        sendMessage("recommendations-out-0", new Event<>(Type.DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Flux<Review> getReviews(int productId) {
    String url = REVIEW_SERVICE_URL + "/review?productId=" + productId;
    LOG.debug("Will call the getReviews API on URL: {}", url);

    return webClient.get().uri(url)
        .retrieve()
        .bodyToFlux(Review.class)
        .log(LOG.getName(), FINE)
        .onErrorResume(error -> empty());
  }

  @Override
  public Mono<Review> createReview(Review body) {
    return Mono.fromCallable(() -> {
      sendMessage("reviews-out-0", new Event<>(Type.CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {
    return Mono.fromRunnable(() ->
            sendMessage("reviews-out-0", new Event<>(Type.DELETE, productId, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  private void sendMessage(String bindingName, Event event) {
    LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);

    Message message = MessageBuilder.withPayload(event)
        .setHeader("partitionKey", event.getKey())
        .build();

    streamBridge.send(bindingName, message);
  }

  private Throwable handleException(Throwable ex) {

    if (!(ex instanceof WebClientResponseException)) {
      LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
      return ex;
    }

    WebClientResponseException wcre = (WebClientResponseException)ex;

    switch (HttpStatus.resolve(wcre.getStatusCode().value())) {

      case NOT_FOUND:
        return new NotFoundException(getErrorMessage(wcre));

      case UNPROCESSABLE_ENTITY:
        return new InvalidInputException(getErrorMessage(wcre));

      default:
        LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
        LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
        return ex;
    }
  }

  private String getErrorMessage(WebClientResponseException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (IOException e) {
      return ex.getMessage();
    }
  }
}
