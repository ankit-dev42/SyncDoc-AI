package com.syncdoc.collaboration.messaging.mapper;

import com.syncdoc.collaboration.messaging.dto.MessageDto;
import com.syncdoc.collaboration.messaging.model.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MessageMapper {

    public MessageDto toDto(Message message) {
        if (message == null) {
            return null;
        }
        return new MessageDto(message);
    }

    public List<MessageDto> toDtoList(List<Message> messages) {
        if (messages == null) {
            return null;
        }
        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Message toEntity(MessageDto dto) {
        if (dto == null) {
            return null;
        }

        Message message = new Message();
        message.setId(dto.getId());
        message.setWorkspaceId(dto.getWorkspaceId());
        message.setChannelId(dto.getChannelId());
        message.setSenderId(dto.getSenderId());
        message.setContent(dto.getContent());
        message.setSequenceNumber(dto.getSequenceNumber());
        message.setIdempotencyKey(dto.getIdempotencyKey());
        message.setMessageType(dto.getMessageType());
        message.setParentMessageId(dto.getParentMessageId());
        message.setEditedAt(dto.getEditedAt());
        message.setDeletedAt(dto.getDeletedAt());
        message.setCreatedAt(dto.getCreatedAt());
        message.setUpdatedAt(dto.getUpdatedAt());

        return message;
    }

    public List<Message> toEntityList(List<MessageDto> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}