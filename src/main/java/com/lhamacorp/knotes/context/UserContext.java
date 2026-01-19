package com.lhamacorp.knotes.context;

import java.util.List;

public record UserContext(String id, String username, List<String> roles) {
}
