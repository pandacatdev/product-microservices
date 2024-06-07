package study.microservices.core.recommendation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import study.api.core.recommendation.Recommendation;
import study.api.core.recommendation.RecommendationService;
import study.api.exceptions.InvalidInputException;
import study.microservices.core.recommendation.persistence.RecommendationEntity;
import study.microservices.core.recommendation.persistence.RecommendationRepository;
import study.util.http.ServiceUtil;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

  private final ServiceUtil serviceUtil;

  private final RecommendationRepository repository;

  private final RecommendationMapper mapper;


  @Autowired
  public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper,
      ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<Recommendation> getRecommendations(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    List<RecommendationEntity> entityList = repository.findByProductId(productId);
    List<Recommendation> list = mapper.entityListToApiList(entityList);
    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

    LOG.debug("getRecommendations: response size: {}", list.size());

    return list;
  }

  @Override
  public Recommendation createRecommendation(Recommendation body) {
    try {
      RecommendationEntity savedEntity = repository.save(mapper.apiToEntity(body));

      LOG.debug("createRecommendation: created a recommendation entity: {}/{}",
          savedEntity.getProductId(), savedEntity.getRecommendationId());
      return mapper.entityToApi(savedEntity);
    } catch (DuplicateKeyException e) {
      throw new InvalidInputException(
          "Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:"
              + body.getRecommendationId());
    }
  }

  @Override
  public void deleteRecommendations(int productId) {
    LOG.debug(
        "deleteRecommendations: tries to delete recommendations for the product with productId: {}",
        productId);
    repository.deleteAll(repository.findByProductId(productId));
  }
}
