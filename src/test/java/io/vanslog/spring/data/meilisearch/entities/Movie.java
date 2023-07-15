package io.vanslog.spring.data.meilisearch.entities;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import org.springframework.data.annotation.Id;

@Document(indexUid = "movies")
@SuppressWarnings("unused")
public class Movie {

  @Id
  private String id;
  private String title;
  private String description;
  private String[] genres;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String[] getGenres() {
    return genres;
  }

  public void setGenres(String[] genres) {
    this.genres = genres;
  }
}
