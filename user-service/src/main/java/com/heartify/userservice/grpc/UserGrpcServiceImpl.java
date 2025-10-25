package com.heartify.userservice.grpc;

import com.heartify.userservice.entity.User;
import com.heartify.userservice.repository.UserRepository;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
public class UserGrpcServiceImpl extends UserGrpcServiceGrpc.UserGrpcServiceImplBase {

    private final UserRepository userRepository;

    public UserGrpcServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getUserById(GetUserByIdRequest request, StreamObserver<UserResponse> responseObserver) {
        UUID userId = UUID.fromString(request.getUserId());
        User user = userRepository.findById(userId).orElse(null);
        
        UserResponse.Builder responseBuilder = UserResponse.newBuilder();
        
        if (user != null) {
            responseBuilder
                .setUserId(user.getId().toString())
                .setEmail(user.getEmail())
                .setRole(user.getRole().name())
                .setIsVerified(user.getIsVerified())
                .setStatus(user.getStatus().name());
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void validateUser(ValidateUserRequest request, StreamObserver<ValidateUserResponse> responseObserver) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        
        ValidateUserResponse.Builder responseBuilder = ValidateUserResponse.newBuilder();
        
        if (user != null && user.getIsVerified() && user.getStatus() == User.UserStatus.ACTIVE) {
            responseBuilder
                .setValid(true)
                .setUserId(user.getId().toString())
                .setRole(user.getRole().name());
        } else {
            responseBuilder.setValid(false);
        }
        
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}