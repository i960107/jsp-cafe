package com.codesquad.cafe.db;

import com.codesquad.cafe.exception.DataIntegrationException;
import com.codesquad.cafe.db.entity.Post;
import com.codesquad.cafe.db.entity.PostDetailsDto;
import com.codesquad.cafe.db.entity.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryPostRepository implements PostRepository {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<Long, Post> posts;

    private AtomicLong seq;

    private UserRepository userRepository;

    public InMemoryPostRepository(UserRepository userRepository) {
        posts = new ConcurrentHashMap<>();
        seq = new AtomicLong(1);
        this.userRepository = userRepository;
    }

    @Override
    public Post save(Post post) {
        post.setId(seq.getAndIncrement());
        posts.put(post.getId(), post);
        return post;
    }

    @Override
    public Optional<Post> findById(Long id) {
        return Optional.ofNullable(posts.get(id));
    }

    @Override
    public List<Post> findAll() {
        return posts.values().stream().toList();
    }

    @Override
    public int countAll() {
        return posts.size();
    }

    @Override
    public Page<PostDetailsDto> findPostWithAuthorByPageSortByCreatedAtDesc(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page num and page size should be greater than 0");
        }

        int totalElements = countAll();

        int start = (pageNum - 1) * pageSize;
        List<Post> result = posts.values().stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .skip(start)
                .limit(pageSize)
                .toList();

        List<PostDetailsDto> detailsDto = result.stream().map(post -> {
            Optional<User> user = userRepository.findById(post.getAuthorId());
            if (user.isEmpty()) {
                throw new DataIntegrationException("author of post does not exists");
            }
            return new PostDetailsDto(post, user.get());
        }).toList();

        return Page.of(
                detailsDto,
                pageNum,
                pageSize,
                totalElements
        );
    }

    @Override
    public void deleteAll() {
        posts.clear();
    }

    @Override
    public Optional<PostDetailsDto> findPostWithAuthorById(Long id) {
        Optional<Post> post = findById(id);
        if (post.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> author = userRepository.findById(post.get().getId());
        if (author.isEmpty()) {
            throw new DataIntegrationException("author of post does not exists");
        }

        return Optional.of(new PostDetailsDto(post.get(), author.get()));
    }
}
