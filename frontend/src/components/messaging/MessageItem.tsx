import React, { useState } from 'react';
import { Message } from '../../types/message';

interface MessageItemProps {
  message: Message;
  currentUserId: string;
  onEdit: (messageId: string, newContent: string) => void;
  onDelete: (messageId: string) => void;
  onReply: (parentMessageId: string) => void;
}

export const MessageItem: React.FC<MessageItemProps> = ({
  message,
  currentUserId,
  onEdit,
  onDelete,
  onReply,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content);
  const [showActions, setShowActions] = useState(false);

  const isOwnMessage = message.senderId === currentUserId;
  const canEdit = isOwnMessage && !message.isDeleted;
  const canDelete = isOwnMessage && !message.isDeleted;

  const relativeTime = new Date(message.createdAt).toLocaleString();

  const handleEdit = () => {
    if (editContent.trim() && editContent !== message.content) {
      onEdit(message.id, editContent.trim());
    }
    setIsEditing(false);
  };

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this message?')) {
      onDelete(message.id);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleEdit();
    } else if (e.key === 'Escape') {
      setEditContent(message.content);
      setIsEditing(false);
    }
  };

  if (message.isDeleted) {
    return (
      <div className="flex items-center space-x-2 text-gray-400 italic text-sm py-1">
        <span>Message deleted</span>
      </div>
    );
  }

  return (
    <div
      className={`group relative flex space-x-3 hover:bg-gray-50 rounded-lg p-2 -m-2 transition-colors ${
        isOwnMessage ? 'flex-row-reverse space-x-reverse' : ''
      }`}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
    >
      {/* Avatar */}
      <div className="flex-shrink-0">
        <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-white text-sm font-medium">
          {message.senderName?.charAt(0)?.toUpperCase() || message.senderId.charAt(0).toUpperCase()}
        </div>
      </div>

      {/* Message Content */}
      <div className={`flex-1 min-w-0 ${isOwnMessage ? 'text-right' : ''}`}>
        <div className="flex items-baseline space-x-2">
          <span className="text-sm font-medium text-gray-900">
            {message.senderName || message.senderId}
          </span>
          <span className="text-xs text-gray-500">
            {relativeTime}
          </span>
          {message.isEdited && (
            <span className="text-xs text-gray-400">(edited)</span>
          )}
        </div>

        <div className="mt-1">
          {isEditing ? (
            <div className="space-y-2">
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                onKeyDown={handleKeyPress}
                className="w-full p-2 border border-gray-300 rounded-md resize-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                rows={Math.min(editContent.split('\n').length, 5)}
                autoFocus
              />
              <div className="flex space-x-2">
                <button
                  onClick={handleEdit}
                  className="px-3 py-1 bg-blue-500 text-white text-sm rounded hover:bg-blue-600"
                >
                  Save
                </button>
                <button
                  onClick={() => {
                    setEditContent(message.content);
                    setIsEditing(false);
                  }}
                  className="px-3 py-1 bg-gray-300 text-gray-700 text-sm rounded hover:bg-gray-400"
                >
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <div className="text-sm text-gray-900 whitespace-pre-wrap break-words">
              {message.content}
            </div>
          )}
        </div>

        {/* Thread indicator */}
        {message.parentMessageId && (
          <div className="text-xs text-gray-500 mt-1">
            Reply to thread
          </div>
        )}
      </div>

      {/* Action buttons */}
      {showActions && !isEditing && (
        <div className={`flex space-x-1 ${isOwnMessage ? 'flex-row-reverse' : ''}`}>
          <button
            onClick={() => onReply(message.id)}
            className="p-1 text-gray-400 hover:text-gray-600 rounded"
            title="Reply"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
            </svg>
          </button>

          {canEdit && (
            <button
              onClick={() => setIsEditing(true)}
              className="p-1 text-gray-400 hover:text-gray-600 rounded"
              title="Edit"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
          )}

          {canDelete && (
            <button
              onClick={handleDelete}
              className="p-1 text-red-400 hover:text-red-600 rounded"
              title="Delete"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          )}
        </div>
      )}
    </div>
  );
};