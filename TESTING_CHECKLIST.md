# SyncDoc AI - Comprehensive Testing Checklist

> Generated from codebase analysis. Format: `- [ ] Test description - expected result`

---

## Table of Contents
1. [Workspace Management](#workspace-management)
2. [Messaging & Threads](#messaging--threads)
3. [Search Functionality](#search-functionality)
4. [Presence System](#presence-system)
5. [Billing & Subscription](#billing--subscription)
6. [Onboarding Flow](#onboarding-flow)
7. [Authentication & Session Handling](#authentication--session-handling)
8. [API Failure & Loading States](#api-failure--loading-states)
9. [Navigation & Deep Links](#navigation--deep-links)
10. [Offline / No Internet Behavior](#offline--no-internet-behavior)
11. [Performance Checkpoints](#performance-checkpoints)
12. [WebSocket Real-time Features](#websocket-real-time-features)
13. [AI Extraction Features](#ai-extraction-features)
14. [Webhook Integration](#webhook-integration)
15. [Accessibility (WCAG 2.1 AA)](#accessibility-wcag-21-aa)
16. [Internationalization (i18n)](#internationalization-i18n)
17. [Security](#security)
18. [Cross-Browser & Platform Behavior](#cross-browser--platform-behavior)

---

## Workspace Management

### Happy Path
- [ ] Load workspace list on app initialization - workspaces appear with name and member count
- [ ] Switch to a different workspace - activeWorkspaceId updates, workspace header reflects new workspace
- [ ] Persist selected workspace to localStorage - on page refresh, same workspace is selected
- [ ] Display active workspace indicator - selected workspace shows highlighted/selected state
- [ ] Show member count for each workspace - badge displays correct number of members
- [ ] Initialize with userId from localStorage - currentUserId is retrieved and shown in header

### Edge Cases & Error States
- [ ] No workspaces available for user - display "No workspaces available" message
- [ ] Workspace list API returns empty array - show empty state with appropriate message
- [ ] Access workspace user is not member of - display 403 forbidden error: "You do not have access to this workspace. Select another workspace to continue."
- [ ] Switch to workspace that no longer exists - handle gracefully, fall back to first available workspace
- [ ] localStorage unavailable (private browsing) - app continues working with default values
- [ ] workspaceId missing from localStorage - fallback to 'workspace-1' default
- [ ] userId missing from localStorage - fallback to 'user-1' default
- [ ] Rapid workspace switching - no race conditions, final state is correct
- [ ] Workspace list API timeout - show loading state, then error message
- [ ] Invalid workspace ID format - API returns validation error, UI shows error

---

## Messaging & Threads

### Sending Messages (Happy Path)
- [ ] Send a new message - message appears in list with correct content, sender, timestamp
- [ ] Send message with Enter key - message sends without shift
- [ ] Send message with button click - message sends successfully
- [ ] Message appears immediately in UI - optimistic update before server confirmation
- [ ] Idempotency key prevents duplicate sends - double-click doesn't create duplicates
- [ ] Send reply in thread - reply appears under parent message
- [ ] Auto-resize textarea as content grows - textarea height adjusts up to max height
- [ ] Clear input field after sending - input resets to empty state
- [ ] Send message with special characters - HTML entities handled, XSS prevented

### Sending Messages (Edge Cases)
- [ ] Send empty message - button disabled, no API call made
- [ ] Send whitespace-only message - treated as empty, not sent
- [ ] Send very long message (10000 chars) - message truncated or error shown at validation
- [ ] Send message exceeding limit - validation error displayed
- [ ] Network failure during send - show error, allow retry
- [ ] Message send timeout - handle gracefully, show retry option
- [ ] Input disabled state - cannot type or send when disabled=true
- [ ] Rapid message sending - all messages queued and sent in order

### Message List (Happy Path)
- [ ] Load initial message history - messages appear in chronological order
- [ ] Display sender name/ID - shows senderName if available, fallback to senderId
- [ ] Display message timestamp - formatted locale string shown
- [ ] Display edited indicator - "(edited)" shown for edited messages
- [ ] Load earlier messages - "Load earlier messages" button fetches older messages
- [ ] Auto-scroll to new messages - new messages scroll into view
- [ ] Preserve scroll position when loading earlier - doesn't jump to bottom

### Message List (Edge Cases)
- [ ] No messages in channel - show "No messages yet. Start the conversation!" or empty state
- [ ] Very long message content - text wraps properly, doesn't overflow
- [ ] Messages with line breaks - whitespace preserved, proper rendering
- [ ] Scroll up disables auto-scroll - manual scroll position preserved
- [ ] Scroll to bottom re-enables auto-scroll - auto-scroll resumes
- [ ] hasMore=false - "Load earlier messages" button hidden
- [ ] Loading more messages - "Loading more..." indicator shown

### Message Editing (Happy Path)
- [ ] Edit own message - edit mode appears with textarea
- [ ] Save edited message - updated content shown, "(edited)" indicator appears
- [ ] Cancel edit with Escape key - reverts to original content
- [ ] Cancel edit with button - reverts to original content
- [ ] Edit with Enter key saves - message updated without shift

### Message Editing (Edge Cases)
- [ ] Cannot edit other user's messages - edit button not shown
- [ ] Cannot edit deleted messages - edit button not shown
- [ ] Edit to empty content - validation prevents save or shows error
- [ ] Edit to same content - no API call made or shows "no changes"
- [ ] Network failure during edit - show error, preserve edit state
- [ ] Concurrent edit conflict - handle gracefully

### Message Deletion (Happy Path)
- [ ] Delete own message - confirmation dialog appears
- [ ] Confirm deletion - message shows "Message deleted" italic text
- [ ] Deleted message displays correctly - shows deleted placeholder

### Message Deletion (Edge Cases)
- [ ] Cannot delete other user's messages - delete button not shown
- [ ] Cancel deletion - message remains unchanged
- [ ] Network failure during delete - show error, message not deleted
- [ ] Delete already deleted message - handle gracefully

### Thread Features (Happy Path)
- [ ] View thread list - threads display with rootMessageId and reply count
- [ ] Click thread to open - thread panel shows with replies
- [ ] Thread reply count badge - shows correct number of replies
- [ ] Reply to thread - reply appears in thread view
- [ ] Cancel reply - reply indicator dismisses

### Thread Features (Edge Cases)
- [ ] No active threads - show "No active threads." message
- [ ] Thread with no replies - show "No replies yet." message
- [ ] Load thread that doesn't exist - show error message
- [ ] Network failure loading thread - show error, allow retry
- [ ] Thread indicator shows unread count - badge appears with new reply count

---

## Search Functionality

### Happy Path
- [ ] Enter search query and click Search - results appear with snippets
- [ ] Search with "from:" filter - only messages from specified user shown
- [ ] Search with "in:" filter - search scoped to specified channel
- [ ] Search with "before:" date filter - only messages before date shown
- [ ] Search with "after:" date filter - only messages after date shown
- [ ] Combine multiple filters - all filters applied correctly
- [ ] Click search result - navigates to message location via URL hash
- [ ] Display search result snippet - highlighted matching text shown
- [ ] Display sender and timestamp - metadata shown for each result

### Edge Cases & Error States
- [ ] Empty search query - results cleared, no API call
- [ ] Search with no results - show "No results." message
- [ ] Search query too short - handle minimum length if required
- [ ] Search query with special characters - properly escaped
- [ ] Invalid date format in filter - show validation error or ignore
- [ ] Search API timeout - show loading, then error
- [ ] Search API error - show "Search request failed" message
- [ ] Very long search query - handled without truncation issues
- [ ] Rapid search submissions - debounced, no race conditions
- [ ] Clear filters after search - filters reset to empty

---

## Presence System

### Happy Path
- [ ] Load presence list on mount - users with statuses displayed
- [ ] Display presence badge (ONLINE) - green badge shown
- [ ] Display presence badge (AWAY) - amber/yellow badge shown
- [ ] Display presence badge (OFFLINE) - grey badge shown
- [ ] Heartbeat sent every 30 seconds - presence stays active
- [ ] Refresh presence list - updated statuses fetched
- [ ] Change own status - dropdown allows selection
- [ ] Status persists in localStorage - preference restored on reload

### Presence Dropdown
- [ ] Open dropdown with click - options appear
- [ ] Select status with click - status changes, dropdown closes
- [ ] Select status with Enter/Space - keyboard accessible
- [ ] Close dropdown with Escape - dropdown closes, focus returns
- [ ] Show current status as selected - aria-selected=true

### Edge Cases & Error States
- [ ] No users with presence - show appropriate empty state
- [ ] Change status API failure - show error, revert optimistic update
- [ ] Heartbeat API failure - handle silently, retry on next interval
- [ ] Presence list API timeout - show loading, then error
- [ ] localStorage unavailable for preference - works without persistence
- [ ] Rapid status changes - last status wins, no race condition
- [ ] User goes offline - status updates to OFFLINE after timeout

### Unread Count
- [ ] Fetch unread count - correct count returned based on lastReadSequence
- [ ] Display unread badge - orange badge with count shown
- [ ] Clear unread on channel view - count resets to 0
- [ ] Increment unread on new message - count increases

---

## Billing & Subscription

### Upgrade Flow (Happy Path)
- [ ] Click "Upgrade to Pro" button - status changes to 'redirecting'
- [ ] Button shows "Redirecting to secure checkout…" - loading text appears
- [ ] API returns checkout URL - browser redirects to Stripe
- [ ] Return from Stripe to /success - PaymentSuccessBanner displayed
- [ ] Success banner shows title - "Payment successful" heading visible
- [ ] Success banner shows body - "Your subscription has been updated successfully." text
- [ ] "Go to dashboard" link works - navigates to home

### Upgrade Flow (Edge Cases)
- [ ] Checkout API failure - show error message
- [ ] Network timeout during checkout - show "Failed to start checkout" error
- [ ] Button disabled while redirecting - prevents double-click
- [ ] Session already upgraded - handle gracefully
- [ ] Invalid userId - API returns error, UI shows error
- [ ] Stripe redirect with invalid session_id - handle gracefully

### Subscription Status
- [ ] Get subscription tier - tier displayed correctly (FREE/PRO)
- [ ] Check can-sync authorization - authorized/blocked based on limits
- [ ] Free tier sync limit reached - show "Sync blocked due to free-tier limit. Upgrade to continue."

### Payment Success Page
- [ ] /success route renders banner - PaymentSuccessBanner component shown
- [ ] Accessibility: role="status" - screen readers announce
- [ ] Accessibility: aria-live="polite" - non-intrusive announcement
- [ ] Checkmark icon visible - green checkmark displayed
- [ ] Complete flow under 30 seconds - performance requirement met

---

## Onboarding Flow

### Happy Path
- [ ] First-time user sees onboarding - modal appears
- [ ] Step 1: "Send your first message" - content and emoji displayed
- [ ] Step 2: "Set your status" - content and emoji displayed
- [ ] Step 3: "Search everything" - content and emoji displayed
- [ ] Progress indicator shows current step - dots fill progressively
- [ ] Click "Next" advances step - next step content appears
- [ ] Click "Got it, let's go!" on last step - modal closes
- [ ] Click "Skip tour" - modal closes immediately
- [ ] Onboarding marked complete in localStorage - doesn't show again

### Edge Cases & Error States
- [ ] Returning user doesn't see onboarding - modal hidden on subsequent visits
- [ ] localStorage unavailable - onboarding shows every time
- [ ] Modal has proper focus trap - Tab cycles within modal
- [ ] Escape key support - should close modal if implemented
- [ ] aria-modal="true" - prevents interaction with background

---

## Authentication & Session Handling

### Happy Path
- [ ] Authorization header added to API requests - Bearer token sent
- [ ] X-Workspace-Id header attached - workspace context sent
- [ ] X-User-Id header attached - user context sent
- [ ] Set auth token via API client - token persisted in headers
- [ ] Valid token allows API access - requests succeed

### Edge Cases & Error States
- [ ] Missing auth token - API returns 401 Unauthorized
- [ ] Expired auth token - show "Your session has expired. Please log in again."
- [ ] Invalid auth token - API returns 401, redirect to login
- [ ] Token refresh flow - if implemented, token refreshes transparently
- [ ] Missing workspace header - API may fail or use defaults
- [ ] Rate limited - show "Too many requests. Please wait a moment and try again."
- [ ] 403 Forbidden - show "You don't have permission to do that."

---

## API Failure & Loading States

### Loading States
- [ ] Workspace list loading - "Loading workspaces..." or skeleton shown
- [ ] Thread list loading - "Loading threads..." shown
- [ ] Thread replies loading - "Loading thread context..." shown
- [ ] Message history loading - "Loading message history..." shown
- [ ] Search loading - "Searching..." shown
- [ ] Presence list loading - "Loading presence..." shown
- [ ] Skeleton loader visible - animated placeholder rows

### Error States
- [ ] Workspace list error - error message in red background
- [ ] Thread list error - error shown with retry option
- [ ] Message list error - red error banner displayed
- [ ] Search error - red error banner displayed
- [ ] Presence error - error message shown
- [ ] Generic error - "Something went wrong. Please try again."

### Network Failure Handling
- [ ] API timeout (all endpoints) - loading indicator, then timeout error
- [ ] Connection refused - appropriate network error
- [ ] DNS resolution failure - network error shown
- [ ] Retry mechanism - where implemented, retries appropriately
- [ ] Offline detection - if implemented, show offline banner

---

## Navigation & Deep Links

### URL Hash Navigation
- [ ] Navigate to search result - hash updates: #workspace=X&channel=Y&seq=Z
- [ ] Parse URL hash on load - navigate to specified message
- [ ] Invalid hash parameters - handle gracefully, show error or default view

### Route Handling
- [ ] /success route renders PaymentSuccessBanner - payment flow works
- [ ] Unknown routes - handled appropriately (404 or redirect)
- [ ] Query string parameters - session_id parsed from Stripe redirect

### Deep Link Edge Cases
- [ ] Deep link to inaccessible workspace - show 403 error
- [ ] Deep link to deleted message - show "not found" or navigate to channel
- [ ] Deep link with malformed parameters - sanitize input, prevent XSS

---

## Offline / No Internet Behavior

### Offline Detection
- [ ] Network goes offline - detect and show error banner if implemented
- [ ] Network reconnects - resume operations, clear error
- [ ] Offline message queue - if implemented, messages queued for send

### Graceful Degradation
- [ ] Cached data remains visible - previously loaded data shown
- [ ] Heartbeat fails silently - doesn't spam errors
- [ ] Manual refresh shows error - "Connection lost" message
- [ ] Auto-reconnect for WebSocket - if implemented, reconnects automatically

### localStorage Fallbacks
- [ ] App works with localStorage - persistent state maintained
- [ ] App works without localStorage - uses in-memory defaults
- [ ] localStorage quota exceeded - handle gracefully

---

## Performance Checkpoints

### Initial Load
- [ ] App renders within acceptable time - no blocking on heavy components
- [ ] Lazy loading works - heavy modules loaded on demand
- [ ] Suspense fallback shows - SkeletonLoader during lazy load
- [ ] No largest contentful paint >2.5s - LCP performance met

### Runtime Performance
- [ ] Smooth scrolling in message list - no jank during scroll
- [ ] Typing indicator doesn't block input - responsive textarea
- [ ] Large message lists perform - virtualization if needed
- [ ] Search debounced - no excessive API calls
- [ ] Memory doesn't leak - no unbounded growth

### API Performance
- [ ] Messages paginated - not loading all at once
- [ ] Search paginated - results limited per request
- [ ] Heartbeat interval appropriate - 30s doesn't overload server

### Bundle Size
- [ ] Code splitting works - separate chunks for features
- [ ] No unnecessary dependencies loaded - tree shaking effective

---

## WebSocket Real-time Features

### Connection Management
- [ ] WebSocket connects on app load - connection established
- [ ] WebSocket reconnects on disconnect - automatic reconnection
- [ ] Connection status indicator - if shown, reflects actual state

### Real-time Messaging
- [ ] Receive new messages in real-time - appears without refresh
- [ ] Receive message edits in real-time - updated content shown
- [ ] Receive message deletes in real-time - deletion shown
- [ ] Typing indicators work - shows when others typing

### Real-time Presence
- [ ] Receive presence updates - other users' status changes shown
- [ ] Own presence broadcast - others see your status change

### Edge Cases
- [ ] WebSocket connection failure - fallback to polling if implemented
- [ ] Message arrives while offline - queued and delivered on reconnect
- [ ] Duplicate message prevention - idempotency key checked
- [ ] Out-of-order messages - sequence numbers enforce ordering

---

## AI Extraction Features

### Happy Path
- [ ] Get extraction status - status returned (PENDING/PROCESSING/COMPLETED)
- [ ] Get extraction result - keyChanges, actionItems, qualityScore returned
- [ ] Status shows COMPLETED - results ready for retrieval

### Edge Cases
- [ ] Get status for non-existent docId - 404 or error returned
- [ ] Get result for incomplete extraction - appropriate error
- [ ] Extraction times out - status reflects timeout state
- [ ] Invalid docId format - validation error returned

---

## Webhook Integration

### GitHub Webhook (Happy Path)
- [ ] Valid signature accepted - webhook processed, 202 Accepted returned
- [ ] Webhook audit logged - accepted events recorded
- [ ] Event dispatched correctly - appropriate handler invoked

### GitHub Webhook (Error Cases)
- [ ] Invalid HMAC signature - 403 INVALID_WEBHOOK_SIGNATURE returned
- [ ] Missing X-Hub-Signature-256 header - request rejected
- [ ] Malformed payload - handle gracefully
- [ ] Rejected webhook audited - rejection recorded with reason

---

## Accessibility (WCAG 2.1 AA)

### Keyboard Navigation
- [ ] All interactive elements focusable - Tab navigates through UI
- [ ] Focus visible on all elements - focus ring shown
- [ ] Escape closes modals/dropdowns - standard keyboard interaction
- [ ] Enter/Space activates buttons - keyboard accessible
- [ ] No keyboard traps - can always navigate away

### Screen Reader Support
- [ ] role="status" on loading indicators - announced appropriately
- [ ] aria-label on SkeletonLoader - "Loading…" announced
- [ ] aria-live="polite" on status changes - updates announced
- [ ] aria-modal="true" on dialogs - modal semantics
- [ ] aria-expanded on dropdowns - state announced
- [ ] aria-selected on list items - selection announced
- [ ] sr-only class hides visually - content available to readers

### Focus Management
- [ ] Focus trap in modals - focus cycles within modal
- [ ] Focus restored after modal close - returns to trigger element
- [ ] Onboarding modal traps focus - Tab stays within tour

### Color & Contrast
- [ ] Text meets 4.5:1 contrast ratio - readable text
- [ ] Interactive elements meet 3:1 - buttons/links visible
- [ ] Color not sole indicator - icons/text supplement color

---

## Internationalization (i18n)

### Translation Coverage
- [ ] All UI strings in en.json - no hardcoded text
- [ ] common.loading translates - "Loading…" shown
- [ ] common.error translates - error message localized
- [ ] presence.status.ONLINE translates - "Online" shown
- [ ] presence.status.AWAY translates - "Away" shown
- [ ] presence.status.OFFLINE translates - "Do not disturb" shown
- [ ] messaging.inputPlaceholder translates - "Type a message…"
- [ ] billing.upgradeToPro translates - "Upgrade to Pro"
- [ ] billing.paymentSuccessTitle translates - "Payment successful"
- [ ] errors.forbidden translates - "You don't have permission to do that."

### Variable Interpolation
- [ ] {{count}} in threadReplies works - "5 replies" displayed
- [ ] {{time}} in lastSeen works - "Last seen 5 min ago"
- [ ] {{query}} in search.noResults works - "No results found for "X""

### Edge Cases
- [ ] Missing translation key - key itself shown as fallback
- [ ] Invalid locale - fallback to 'en'
- [ ] Special characters in translations - properly escaped

---

## Security

### Input Validation
- [ ] Message content sanitized - no XSS in rendered content
- [ ] Search query sanitized - no injection attacks
- [ ] URL parameters sanitized - no XSS via deep links
- [ ] senderId validated (max 50 chars) - validation enforced
- [ ] content validated (max 10000 chars) - validation enforced

### Authentication
- [ ] Unauthenticated requests blocked - @PreAuthorize("isAuthenticated()") works
- [ ] Token stored securely - not exposed in URL or logs
- [ ] CSRF protection - if applicable, tokens validated

### Data Protection
- [ ] Workspace isolation - users only see their workspaces
- [ ] Message access control - can't read other workspace messages
- [ ] Presence scoped to workspace - can't see other workspace presence

### Webhook Security
- [ ] HMAC signature verified - invalid signatures rejected
- [ ] Webhook secret not exposed - stored securely
- [ ] Payload audit trail - accepted/rejected webhooks logged

### Rate Limiting
- [ ] Rate limit filter works - excessive requests blocked
- [ ] Rate limit error shown - "Too many requests" message

---

## Cross-Browser & Platform Behavior

### Browser Compatibility
- [ ] Chrome latest - all features work
- [ ] Firefox latest - all features work
- [ ] Safari latest - all features work
- [ ] Edge latest - all features work

### Mobile Responsiveness
- [ ] Layout adapts to mobile - grid collapses appropriately
- [ ] Touch interactions work - buttons/links tappable
- [ ] Mobile keyboard doesn't break layout - input visible above keyboard
- [ ] Textarea auto-resize works on mobile - height adjusts

### localStorage Behavior
- [ ] Works in normal mode - persistence functional
- [ ] Works in private/incognito - graceful degradation
- [ ] Works when storage full - error handled

---

## Backend API Validation

### Message Controller
- [ ] POST /messages with valid data - 200 with message returned
- [ ] POST /messages with empty content - 400 validation error
- [ ] POST /messages with content >10000 chars - 400 validation error
- [ ] POST /messages with senderId >50 chars - 400 validation error
- [ ] GET /messages with pagination - correct page returned
- [ ] GET /messages/after/{seq} - messages after sequence returned
- [ ] GET /messages/before/{seq} - messages before sequence returned
- [ ] PUT /messages/{id} with valid data - message updated
- [ ] PUT /messages/{id} by non-owner - 403 forbidden
- [ ] DELETE /messages/{id} by owner - message deleted
- [ ] DELETE /messages/{id} by non-owner - 403 forbidden
- [ ] GET /messages/search with query - results returned
- [ ] GET /messages/thread/{parentId} - replies returned
- [ ] GET /messages/threads - thread list returned
- [ ] GET /messages/count - correct count returned

### Presence Controller
- [ ] POST /presence/status - presence updated
- [ ] POST /presence/heartbeat - heartbeat accepted
- [ ] GET /presence - presence list returned
- [ ] GET /presence/unread-count - correct count returned

### Search Controller
- [ ] GET /search with query - results with snippets
- [ ] GET /search with from filter - filtered by sender
- [ ] GET /search with before filter - filtered by date
- [ ] GET /search with after filter - filtered by date
- [ ] GET /search with invalid date - handled gracefully

### Subscription Controller
- [ ] GET /subscriptions/{userId}/tier - tier info returned
- [ ] POST /subscriptions/{userId}/can-sync - authorization evaluated

### Workspace Controller
- [ ] GET /workspaces - user's workspaces returned
- [ ] POST /workspaces/{id}/members/{userId} - member added
- [ ] DELETE /workspaces/{id}/members/{userId} - member removed

### AI Controller
- [ ] GET /ai/extract-status/{docId} - status returned
- [ ] GET /ai/extract-result/{docId} - result returned for completed

### Webhook Controller
- [ ] POST /webhooks/github with valid signature - 202 Accepted
- [ ] POST /webhooks/github with invalid signature - 403 error

---

## Data Consistency & Edge Cases

### Idempotency
- [ ] Duplicate message with same idempotencyKey - no duplicate created
- [ ] Retry failed request with idempotencyKey - same result

### Concurrent Operations
- [ ] Two users edit same message - last write wins or conflict resolution
- [ ] Message deleted while being edited - handle gracefully
- [ ] Workspace deleted while active - user redirected/notified

### Data Integrity
- [ ] Sequence numbers are unique - no duplicates in channel
- [ ] Sequence numbers are monotonic - always increasing
- [ ] Timestamps are correct - server time used

---

## Chaos & Resilience Testing

### Backend Resilience
- [ ] Database connection lost - graceful degradation, error shown
- [ ] Redis cache unavailable - falls back or shows error
- [ ] Stripe API unavailable - checkout fails gracefully
- [ ] GitHub unavailable - webhook timeout handled

### Network Partition
- [ ] Frontend loses backend - shows reconnecting/error state
- [ ] WebSocket disconnects - attempts reconnection
- [ ] Partial response received - handle incomplete data

---

*Total test cases: ~350+*

**Usage:** Copy this checklist into your test management tool or review manually before releases.
