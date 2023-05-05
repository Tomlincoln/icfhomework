package hu.tomlincoln.icfhomework.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import hu.tomlincoln.icfhomework.entity.Artwork;

@Repository
public interface ArtworkRepository extends PagingAndSortingRepository<Artwork, Integer> {


}
