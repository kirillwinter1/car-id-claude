package ru.car.service.message.telegram.scene.state;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SceneStateRegistry {

    public record PendingText(String scene, String action, List<String> args, String draftText, Instant expiresAt) {}

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    private final Map<Long, PendingText> states = new ConcurrentHashMap<>();

    public void put(long chatId, String scene, String action, List<String> args) {
        putWithTtl(chatId, scene, action, args, DEFAULT_TTL);
    }

    public void putWithTtl(long chatId, String scene, String action, List<String> args, Duration ttl) {
        states.put(chatId, new PendingText(scene, action, List.copyOf(args), null, Instant.now().plus(ttl)));
    }

    public void updateDraft(long chatId, String draftText) {
        states.computeIfPresent(chatId, (k, v) ->
            new PendingText(v.scene(), v.action(), v.args(), draftText, v.expiresAt()));
    }

    public Optional<PendingText> peek(long chatId) {
        PendingText current = states.get(chatId);
        if (current == null) return Optional.empty();
        if (Instant.now().isAfter(current.expiresAt())) {
            states.remove(chatId);
            return Optional.empty();
        }
        return Optional.of(current);
    }

    public Optional<PendingText> pop(long chatId) {
        Optional<PendingText> current = peek(chatId);
        current.ifPresent(c -> states.remove(chatId));
        return current;
    }

    public void clear(long chatId) {
        states.remove(chatId);
    }
}
