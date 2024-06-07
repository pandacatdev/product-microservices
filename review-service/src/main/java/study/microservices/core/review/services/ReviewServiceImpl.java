package study.microservices.core.review.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import study.api.core.review.Review;
import study.api.core.review.ReviewService;
import study.api.exceptions.InvalidInputException;
import study.microservices.core.review.persistence.ReviewEntity;
import study.microservices.core.review.persistence.ReviewRepository;
import study.util.http.ServiceUtil;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ReviewServiceImpl implements ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

  private final ServiceUtil serviceUtil;

  private final ReviewRepository repository;

  private final ReviewMapper mapper;

  @Autowired
  public ReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper,
      ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<Review> getReviews(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    List<ReviewEntity> entityList = repository.findByProductId(productId);
    List<Review> list = mapper.entityListToApiList(entityList);
    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

    LOG.debug("getReviews: response size: {}", list.size());

    return list;
  }

  @Override
  public Review createReview(Review body) {
    try {
      ReviewEntity savedEntity = repository.save(mapper.apiToEntity(body));

      LOG.debug("createReview: created a review entity: {}/{}", savedEntity.getProductId(),
          savedEntity.getReviewId());
      return mapper.entityToApi(savedEntity);

    } catch (DataIntegrityViolationException dive) {
      throw new InvalidInputException(
          "Duplicate key, Product Id: " + body.getProductId() + ", Review Id:"
              + body.getReviewId());
    }
  }

  @Override
  public void deleteReviews(int productId) {
    LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}",
        productId);
    repository.deleteAll(repository.findByProductId(productId));
  }
}
