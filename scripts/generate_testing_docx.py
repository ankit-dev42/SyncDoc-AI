#!/usr/bin/env python3
"""
Generate a professional Word document from the TESTING_CHECKLIST.md file.
"""

from docx import Document
from docx.shared import Inches, Pt, RGBColor, Twips
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import re
from datetime import datetime

# Define sections data structure (parsed from the markdown)
SECTIONS = [
    {
        "name": "Workspace Management",
        "subsections": [
            {
                "name": "Happy Path",
                "tests": [
                    ("Load workspace list on app initialization", "workspaces appear with name and member count"),
                    ("Switch to a different workspace", "activeWorkspaceId updates, workspace header reflects new workspace"),
                    ("Persist selected workspace to localStorage", "on page refresh, same workspace is selected"),
                    ("Display active workspace indicator", "selected workspace shows highlighted/selected state"),
                    ("Show member count for each workspace", "badge displays correct number of members"),
                    ("Initialize with userId from localStorage", "currentUserId is retrieved and shown in header"),
                ]
            },
            {
                "name": "Edge Cases & Error States",
                "tests": [
                    ("No workspaces available for user", 'display "No workspaces available" message'),
                    ("Workspace list API returns empty array", "show empty state with appropriate message"),
                    ("Access workspace user is not member of", 'display 403 forbidden error: "You do not have access to this workspace."'),
                    ("Switch to workspace that no longer exists", "handle gracefully, fall back to first available workspace"),
                    ("localStorage unavailable (private browsing)", "app continues working with default values"),
                    ("workspaceId missing from localStorage", "fallback to 'workspace-1' default"),
                    ("userId missing from localStorage", "fallback to 'user-1' default"),
                    ("Rapid workspace switching", "no race conditions, final state is correct"),
                    ("Workspace list API timeout", "show loading state, then error message"),
                    ("Invalid workspace ID format", "API returns validation error, UI shows error"),
                ]
            }
        ]
    },
    {
        "name": "Messaging & Threads",
        "subsections": [
            {
                "name": "Sending Messages (Happy Path)",
                "tests": [
                    ("Send a new message", "message appears in list with correct content, sender, timestamp"),
                    ("Send message with Enter key", "message sends without shift"),
                    ("Send message with button click", "message sends successfully"),
                    ("Message appears immediately in UI", "optimistic update before server confirmation"),
                    ("Idempotency key prevents duplicate sends", "double-click doesn't create duplicates"),
                    ("Send reply in thread", "reply appears under parent message"),
                    ("Auto-resize textarea as content grows", "textarea height adjusts up to max height"),
                    ("Clear input field after sending", "input resets to empty state"),
                    ("Send message with special characters", "HTML entities handled, XSS prevented"),
                ]
            },
            {
                "name": "Sending Messages (Edge Cases)",
                "tests": [
                    ("Send empty message", "button disabled, no API call made"),
                    ("Send whitespace-only message", "treated as empty, not sent"),
                    ("Send very long message (10000 chars)", "message truncated or error shown at validation"),
                    ("Send message exceeding limit", "validation error displayed"),
                    ("Network failure during send", "show error, allow retry"),
                    ("Message send timeout", "handle gracefully, show retry option"),
                    ("Input disabled state", "cannot type or send when disabled=true"),
                    ("Rapid message sending", "all messages queued and sent in order"),
                ]
            },
            {
                "name": "Message List (Happy Path)",
                "tests": [
                    ("Load initial message history", "messages appear in chronological order"),
                    ("Display sender name/ID", "shows senderName if available, fallback to senderId"),
                    ("Display message timestamp", "formatted locale string shown"),
                    ("Display edited indicator", '"(edited)" shown for edited messages'),
                    ("Load earlier messages", '"Load earlier messages" button fetches older messages'),
                    ("Auto-scroll to new messages", "new messages scroll into view"),
                    ("Preserve scroll position when loading earlier", "doesn't jump to bottom"),
                ]
            },
            {
                "name": "Message List (Edge Cases)",
                "tests": [
                    ("No messages in channel", 'show "No messages yet. Start the conversation!" or empty state'),
                    ("Very long message content", "text wraps properly, doesn't overflow"),
                    ("Messages with line breaks", "whitespace preserved, proper rendering"),
                    ("Scroll up disables auto-scroll", "manual scroll position preserved"),
                    ("Scroll to bottom re-enables auto-scroll", "auto-scroll resumes"),
                    ('hasMore=false', '"Load earlier messages" button hidden'),
                    ("Loading more messages", '"Loading more..." indicator shown'),
                ]
            },
            {
                "name": "Message Editing (Happy Path)",
                "tests": [
                    ("Edit own message", "edit mode appears with textarea"),
                    ("Save edited message", 'updated content shown, "(edited)" indicator appears'),
                    ("Cancel edit with Escape key", "reverts to original content"),
                    ("Cancel edit with button", "reverts to original content"),
                    ("Edit with Enter key saves", "message updated without shift"),
                ]
            },
            {
                "name": "Message Editing (Edge Cases)",
                "tests": [
                    ("Cannot edit other user's messages", "edit button not shown"),
                    ("Cannot edit deleted messages", "edit button not shown"),
                    ("Edit to empty content", "validation prevents save or shows error"),
                    ("Edit to same content", 'no API call made or shows "no changes"'),
                    ("Network failure during edit", "show error, preserve edit state"),
                    ("Concurrent edit conflict", "handle gracefully"),
                ]
            },
            {
                "name": "Message Deletion (Happy Path)",
                "tests": [
                    ("Delete own message", "confirmation dialog appears"),
                    ("Confirm deletion", 'message shows "Message deleted" italic text'),
                    ("Deleted message displays correctly", "shows deleted placeholder"),
                ]
            },
            {
                "name": "Message Deletion (Edge Cases)",
                "tests": [
                    ("Cannot delete other user's messages", "delete button not shown"),
                    ("Cancel deletion", "message remains unchanged"),
                    ("Network failure during delete", "show error, message not deleted"),
                    ("Delete already deleted message", "handle gracefully"),
                ]
            },
            {
                "name": "Thread Features (Happy Path)",
                "tests": [
                    ("View thread list", "threads display with rootMessageId and reply count"),
                    ("Click thread to open", "thread panel shows with replies"),
                    ("Thread reply count badge", "shows correct number of replies"),
                    ("Reply to thread", "reply appears in thread view"),
                    ("Cancel reply", "reply indicator dismisses"),
                ]
            },
            {
                "name": "Thread Features (Edge Cases)",
                "tests": [
                    ("No active threads", 'show "No active threads." message'),
                    ("Thread with no replies", 'show "No replies yet." message'),
                    ("Load thread that doesn't exist", "show error message"),
                    ("Network failure loading thread", "show error, allow retry"),
                    ("Thread indicator shows unread count", "badge appears with new reply count"),
                ]
            }
        ]
    },
    {
        "name": "Search Functionality",
        "subsections": [
            {
                "name": "Happy Path",
                "tests": [
                    ("Enter search query and click Search", "results appear with snippets"),
                    ('Search with "from:" filter', "only messages from specified user shown"),
                    ('Search with "in:" filter', "search scoped to specified channel"),
                    ('Search with "before:" date filter', "only messages before date shown"),
                    ('Search with "after:" date filter', "only messages after date shown"),
                    ("Combine multiple filters", "all filters applied correctly"),
                    ("Click search result", "navigates to message location via URL hash"),
                    ("Display search result snippet", "highlighted matching text shown"),
                    ("Display sender and timestamp", "metadata shown for each result"),
                ]
            },
            {
                "name": "Edge Cases & Error States",
                "tests": [
                    ("Empty search query", "results cleared, no API call"),
                    ("Search with no results", 'show "No results." message'),
                    ("Search query too short", "handle minimum length if required"),
                    ("Search query with special characters", "properly escaped"),
                    ("Invalid date format in filter", "show validation error or ignore"),
                    ("Search API timeout", "show loading, then error"),
                    ("Search API error", 'show "Search request failed" message'),
                    ("Very long search query", "handled without truncation issues"),
                    ("Rapid search submissions", "debounced, no race conditions"),
                    ("Clear filters after search", "filters reset to empty"),
                ]
            }
        ]
    },
    {
        "name": "Presence System",
        "subsections": [
            {
                "name": "Happy Path",
                "tests": [
                    ("Load presence list on mount", "users with statuses displayed"),
                    ("Display presence badge (ONLINE)", "green badge shown"),
                    ("Display presence badge (AWAY)", "amber/yellow badge shown"),
                    ("Display presence badge (OFFLINE)", "grey badge shown"),
                    ("Heartbeat sent every 30 seconds", "presence stays active"),
                    ("Refresh presence list", "updated statuses fetched"),
                    ("Change own status", "dropdown allows selection"),
                    ("Status persists in localStorage", "preference restored on reload"),
                ]
            },
            {
                "name": "Presence Dropdown",
                "tests": [
                    ("Open dropdown with click", "options appear"),
                    ("Select status with click", "status changes, dropdown closes"),
                    ("Select status with Enter/Space", "keyboard accessible"),
                    ("Close dropdown with Escape", "dropdown closes, focus returns"),
                    ("Show current status as selected", "aria-selected=true"),
                ]
            },
            {
                "name": "Edge Cases & Error States",
                "tests": [
                    ("No users with presence", "show appropriate empty state"),
                    ("Change status API failure", "show error, revert optimistic update"),
                    ("Heartbeat API failure", "handle silently, retry on next interval"),
                    ("Presence list API timeout", "show loading, then error"),
                    ("localStorage unavailable for preference", "works without persistence"),
                    ("Rapid status changes", "last status wins, no race condition"),
                    ("User goes offline", "status updates to OFFLINE after timeout"),
                ]
            },
            {
                "name": "Unread Count",
                "tests": [
                    ("Fetch unread count", "correct count returned based on lastReadSequence"),
                    ("Display unread badge", "orange badge with count shown"),
                    ("Clear unread on channel view", "count resets to 0"),
                    ("Increment unread on new message", "count increases"),
                ]
            }
        ]
    },
    {
        "name": "Billing & Subscription",
        "subsections": [
            {
                "name": "Upgrade Flow (Happy Path)",
                "tests": [
                    ('Click "Upgrade to Pro" button', "status changes to 'redirecting'"),
                    ('Button shows "Redirecting to secure checkout…"', "loading text appears"),
                    ("API returns checkout URL", "browser redirects to Stripe"),
                    ("Return from Stripe to /success", "PaymentSuccessBanner displayed"),
                    ("Success banner shows title", '"Payment successful" heading visible'),
                    ("Success banner shows body", '"Your subscription has been updated successfully." text'),
                    ('"Go to dashboard" link works', "navigates to home"),
                ]
            },
            {
                "name": "Upgrade Flow (Edge Cases)",
                "tests": [
                    ("Checkout API failure", "show error message"),
                    ("Network timeout during checkout", 'show "Failed to start checkout" error'),
                    ("Button disabled while redirecting", "prevents double-click"),
                    ("Session already upgraded", "handle gracefully"),
                    ("Invalid userId", "API returns error, UI shows error"),
                    ("Stripe redirect with invalid session_id", "handle gracefully"),
                ]
            },
            {
                "name": "Subscription Status",
                "tests": [
                    ("Get subscription tier", "tier displayed correctly (FREE/PRO)"),
                    ("Check can-sync authorization", "authorized/blocked based on limits"),
                    ("Free tier sync limit reached", 'show "Sync blocked due to free-tier limit. Upgrade to continue."'),
                ]
            },
            {
                "name": "Payment Success Page",
                "tests": [
                    ("/success route renders banner", "PaymentSuccessBanner component shown"),
                    ('Accessibility: role="status"', "screen readers announce"),
                    ('Accessibility: aria-live="polite"', "non-intrusive announcement"),
                    ("Checkmark icon visible", "green checkmark displayed"),
                    ("Complete flow under 30 seconds", "performance requirement met"),
                ]
            }
        ]
    },
    {
        "name": "Onboarding Flow",
        "subsections": [
            {
                "name": "Happy Path",
                "tests": [
                    ("First-time user sees onboarding", "modal appears"),
                    ('Step 1: "Send your first message"', "content and emoji displayed"),
                    ('Step 2: "Set your status"', "content and emoji displayed"),
                    ('Step 3: "Search everything"', "content and emoji displayed"),
                    ("Progress indicator shows current step", "dots fill progressively"),
                    ('Click "Next" advances step', "next step content appears"),
                    ('Click "Got it, let\'s go!" on last step', "modal closes"),
                    ('Click "Skip tour"', "modal closes immediately"),
                    ("Onboarding marked complete in localStorage", "doesn't show again"),
                ]
            },
            {
                "name": "Edge Cases & Error States",
                "tests": [
                    ("Returning user doesn't see onboarding", "modal hidden on subsequent visits"),
                    ("localStorage unavailable", "onboarding shows every time"),
                    ("Modal has proper focus trap", "Tab cycles within modal"),
                    ("Escape key support", "should close modal if implemented"),
                    ('aria-modal="true"', "prevents interaction with background"),
                ]
            }
        ]
    },
    {
        "name": "Authentication & Session Handling",
        "subsections": [
            {
                "name": "Happy Path",
                "tests": [
                    ("Authorization header added to API requests", "Bearer token sent"),
                    ("X-Workspace-Id header attached", "workspace context sent"),
                    ("X-User-Id header attached", "user context sent"),
                    ("Set auth token via API client", "token persisted in headers"),
                    ("Valid token allows API access", "requests succeed"),
                ]
            },
            {
                "name": "Edge Cases & Error States",
                "tests": [
                    ("Missing auth token", "API returns 401 Unauthorized"),
                    ("Expired auth token", 'show "Your session has expired. Please log in again."'),
                    ("Invalid auth token", "API returns 401, redirect to login"),
                    ("Token refresh flow", "if implemented, token refreshes transparently"),
                    ("Missing workspace header", "API may fail or use defaults"),
                    ("Rate limited", 'show "Too many requests. Please wait a moment and try again."'),
                    ("403 Forbidden", 'show "You don\'t have permission to do that."'),
                ]
            }
        ]
    },
    {
        "name": "API Failure & Loading States",
        "subsections": [
            {
                "name": "Loading States",
                "tests": [
                    ("Workspace list loading", '"Loading workspaces..." or skeleton shown'),
                    ("Thread list loading", '"Loading threads..." shown'),
                    ("Thread replies loading", '"Loading thread context..." shown'),
                    ("Message history loading", '"Loading message history..." shown'),
                    ("Search loading", '"Searching..." shown'),
                    ("Presence list loading", '"Loading presence..." shown'),
                    ("Skeleton loader visible", "animated placeholder rows"),
                ]
            },
            {
                "name": "Error States",
                "tests": [
                    ("Workspace list error", "error message in red background"),
                    ("Thread list error", "error shown with retry option"),
                    ("Message list error", "red error banner displayed"),
                    ("Search error", "red error banner displayed"),
                    ("Presence error", "error message shown"),
                    ("Generic error", '"Something went wrong. Please try again."'),
                ]
            },
            {
                "name": "Network Failure Handling",
                "tests": [
                    ("API timeout (all endpoints)", "loading indicator, then timeout error"),
                    ("Connection refused", "appropriate network error"),
                    ("DNS resolution failure", "network error shown"),
                    ("Retry mechanism", "where implemented, retries appropriately"),
                    ("Offline detection", "if implemented, show offline banner"),
                ]
            }
        ]
    },
    {
        "name": "Navigation & Deep Links",
        "subsections": [
            {
                "name": "URL Hash Navigation",
                "tests": [
                    ("Navigate to search result", "hash updates: #workspace=X&channel=Y&seq=Z"),
                    ("Parse URL hash on load", "navigate to specified message"),
                    ("Invalid hash parameters", "handle gracefully, show error or default view"),
                ]
            },
            {
                "name": "Route Handling",
                "tests": [
                    ("/success route renders PaymentSuccessBanner", "payment flow works"),
                    ("Unknown routes", "handled appropriately (404 or redirect)"),
                    ("Query string parameters", "session_id parsed from Stripe redirect"),
                ]
            },
            {
                "name": "Deep Link Edge Cases",
                "tests": [
                    ("Deep link to inaccessible workspace", "show 403 error"),
                    ("Deep link to deleted message", 'show "not found" or navigate to channel'),
                    ("Deep link with malformed parameters", "sanitize input, prevent XSS"),
                ]
            }
        ]
    },
    {
        "name": "Offline / No Internet Behavior",
        "subsections": [
            {
                "name": "Offline Detection",
                "tests": [
                    ("Network goes offline", "detect and show error banner if implemented"),
                    ("Network reconnects", "resume operations, clear error"),
                    ("Offline message queue", "if implemented, messages queued for send"),
                ]
            },
            {
                "name": "Graceful Degradation",
                "tests": [
                    ("Cached data remains visible", "previously loaded data shown"),
                    ("Heartbeat fails silently", "doesn't spam errors"),
                    ("Manual refresh shows error", '"Connection lost" message'),
                    ("Auto-reconnect for WebSocket", "if implemented, reconnects automatically"),
                ]
            },
            {
                "name": "localStorage Fallbacks",
                "tests": [
                    ("App works with localStorage", "persistent state maintained"),
                    ("App works without localStorage", "uses in-memory defaults"),
                    ("localStorage quota exceeded", "handle gracefully"),
                ]
            }
        ]
    },
    {
        "name": "Performance Checkpoints",
        "subsections": [
            {
                "name": "Initial Load",
                "tests": [
                    ("App renders within acceptable time", "no blocking on heavy components"),
                    ("Lazy loading works", "heavy modules loaded on demand"),
                    ("Suspense fallback shows", "SkeletonLoader during lazy load"),
                    ("No largest contentful paint >2.5s", "LCP performance met"),
                ]
            },
            {
                "name": "Runtime Performance",
                "tests": [
                    ("Smooth scrolling in message list", "no jank during scroll"),
                    ("Typing indicator doesn't block input", "responsive textarea"),
                    ("Large message lists perform", "virtualization if needed"),
                    ("Search debounced", "no excessive API calls"),
                    ("Memory doesn't leak", "no unbounded growth"),
                ]
            },
            {
                "name": "API Performance",
                "tests": [
                    ("Messages paginated", "not loading all at once"),
                    ("Search paginated", "results limited per request"),
                    ("Heartbeat interval appropriate", "30s doesn't overload server"),
                ]
            },
            {
                "name": "Bundle Size",
                "tests": [
                    ("Code splitting works", "separate chunks for features"),
                    ("No unnecessary dependencies loaded", "tree shaking effective"),
                ]
            }
        ]
    },
    {
        "name": "WebSocket Real-time Features",
        "subsections": [
            {
                "name": "Connection Management",
                "tests": [
                    ("WebSocket connects on app load", "connection established"),
                    ("WebSocket reconnects on disconnect", "automatic reconnection"),
                    ("Connection status indicator", "if shown, reflects actual state"),
                ]
            },
            {
                "name": "Real-time Messaging",
                "tests": [
                    ("Receive new messages in real-time", "appears without refresh"),
                    ("Receive message edits in real-time", "updated content shown"),
                    ("Receive message deletes in real-time", "deletion shown"),
                    ("Typing indicators work", "shows when others typing"),
                ]
            },
            {
                "name": "Real-time Presence",
                "tests": [
                    ("Receive presence updates", "other users' status changes shown"),
                    ("Own presence broadcast", "others see your status change"),
                ]
            },
            {
                "name": "Edge Cases",
                "tests": [
                    ("WebSocket connection failure", "fallback to polling if implemented"),
                    ("Message arrives while offline", "queued and delivered on reconnect"),
                    ("Duplicate message prevention", "idempotency key checked"),
                    ("Out-of-order messages", "sequence numbers enforce ordering"),
                ]
            }
        ]
    },
    {
        "name": "AI Extraction Features",
        "subsections": [
            {
                "name": "Happy Path",
                "tests": [
                    ("Get extraction status", "status returned (PENDING/PROCESSING/COMPLETED)"),
                    ("Get extraction result", "keyChanges, actionItems, qualityScore returned"),
                    ("Status shows COMPLETED", "results ready for retrieval"),
                ]
            },
            {
                "name": "Edge Cases",
                "tests": [
                    ("Get status for non-existent docId", "404 or error returned"),
                    ("Get result for incomplete extraction", "appropriate error"),
                    ("Extraction times out", "status reflects timeout state"),
                    ("Invalid docId format", "validation error returned"),
                ]
            }
        ]
    },
    {
        "name": "Webhook Integration",
        "subsections": [
            {
                "name": "GitHub Webhook (Happy Path)",
                "tests": [
                    ("Valid signature accepted", "webhook processed, 202 Accepted returned"),
                    ("Webhook audit logged", "accepted events recorded"),
                    ("Event dispatched correctly", "appropriate handler invoked"),
                ]
            },
            {
                "name": "GitHub Webhook (Error Cases)",
                "tests": [
                    ("Invalid HMAC signature", "403 INVALID_WEBHOOK_SIGNATURE returned"),
                    ("Missing X-Hub-Signature-256 header", "request rejected"),
                    ("Malformed payload", "handle gracefully"),
                    ("Rejected webhook audited", "rejection recorded with reason"),
                ]
            }
        ]
    },
    {
        "name": "Accessibility (WCAG 2.1 AA)",
        "subsections": [
            {
                "name": "Keyboard Navigation",
                "tests": [
                    ("All interactive elements focusable", "Tab navigates through UI"),
                    ("Focus visible on all elements", "focus ring shown"),
                    ("Escape closes modals/dropdowns", "standard keyboard interaction"),
                    ("Enter/Space activates buttons", "keyboard accessible"),
                    ("No keyboard traps", "can always navigate away"),
                ]
            },
            {
                "name": "Screen Reader Support",
                "tests": [
                    ('role="status" on loading indicators', "announced appropriately"),
                    ("aria-label on SkeletonLoader", '"Loading…" announced'),
                    ('aria-live="polite" on status changes', "updates announced"),
                    ('aria-modal="true" on dialogs', "modal semantics"),
                    ("aria-expanded on dropdowns", "state announced"),
                    ("aria-selected on list items", "selection announced"),
                    ("sr-only class hides visually", "content available to readers"),
                ]
            },
            {
                "name": "Focus Management",
                "tests": [
                    ("Focus trap in modals", "focus cycles within modal"),
                    ("Focus restored after modal close", "returns to trigger element"),
                    ("Onboarding modal traps focus", "Tab stays within tour"),
                ]
            },
            {
                "name": "Color & Contrast",
                "tests": [
                    ("Text meets 4.5:1 contrast ratio", "readable text"),
                    ("Interactive elements meet 3:1", "buttons/links visible"),
                    ("Color not sole indicator", "icons/text supplement color"),
                ]
            }
        ]
    },
    {
        "name": "Internationalization (i18n)",
        "subsections": [
            {
                "name": "Translation Coverage",
                "tests": [
                    ("All UI strings in en.json", "no hardcoded text"),
                    ("common.loading translates", '"Loading…" shown'),
                    ("common.error translates", "error message localized"),
                    ("presence.status.ONLINE translates", '"Online" shown'),
                    ("presence.status.AWAY translates", '"Away" shown'),
                    ("presence.status.OFFLINE translates", '"Do not disturb" shown'),
                    ("messaging.inputPlaceholder translates", '"Type a message…"'),
                    ("billing.upgradeToPro translates", '"Upgrade to Pro"'),
                    ("billing.paymentSuccessTitle translates", '"Payment successful"'),
                    ('errors.forbidden translates', '"You don\'t have permission to do that."'),
                ]
            },
            {
                "name": "Variable Interpolation",
                "tests": [
                    ("{{count}} in threadReplies works", '"5 replies" displayed'),
                    ("{{time}} in lastSeen works", '"Last seen 5 min ago"'),
                    ('{{query}} in search.noResults works', '"No results found for \\"X\\""'),
                ]
            },
            {
                "name": "Edge Cases",
                "tests": [
                    ("Missing translation key", "key itself shown as fallback"),
                    ("Invalid locale", "fallback to 'en'"),
                    ("Special characters in translations", "properly escaped"),
                ]
            }
        ]
    },
    {
        "name": "Security",
        "subsections": [
            {
                "name": "Input Validation",
                "tests": [
                    ("Message content sanitized", "no XSS in rendered content"),
                    ("Search query sanitized", "no injection attacks"),
                    ("URL parameters sanitized", "no XSS via deep links"),
                    ("senderId validated (max 50 chars)", "validation enforced"),
                    ("content validated (max 10000 chars)", "validation enforced"),
                ]
            },
            {
                "name": "Authentication",
                "tests": [
                    ("Unauthenticated requests blocked", '@PreAuthorize("isAuthenticated()") works'),
                    ("Token stored securely", "not exposed in URL or logs"),
                    ("CSRF protection", "if applicable, tokens validated"),
                ]
            },
            {
                "name": "Data Protection",
                "tests": [
                    ("Workspace isolation", "users only see their workspaces"),
                    ("Message access control", "can't read other workspace messages"),
                    ("Presence scoped to workspace", "can't see other workspace presence"),
                ]
            },
            {
                "name": "Webhook Security",
                "tests": [
                    ("HMAC signature verified", "invalid signatures rejected"),
                    ("Webhook secret not exposed", "stored securely"),
                    ("Payload audit trail", "accepted/rejected webhooks logged"),
                ]
            },
            {
                "name": "Rate Limiting",
                "tests": [
                    ("Rate limit filter works", "excessive requests blocked"),
                    ("Rate limit error shown", '"Too many requests" message'),
                ]
            }
        ]
    },
    {
        "name": "Cross-Browser & Platform Behavior",
        "subsections": [
            {
                "name": "Browser Compatibility",
                "tests": [
                    ("Chrome latest", "all features work"),
                    ("Firefox latest", "all features work"),
                    ("Safari latest", "all features work"),
                    ("Edge latest", "all features work"),
                ]
            },
            {
                "name": "Mobile Responsiveness",
                "tests": [
                    ("Layout adapts to mobile", "grid collapses appropriately"),
                    ("Touch interactions work", "buttons/links tappable"),
                    ("Mobile keyboard doesn't break layout", "input visible above keyboard"),
                    ("Textarea auto-resize works on mobile", "height adjusts"),
                ]
            },
            {
                "name": "localStorage Behavior",
                "tests": [
                    ("Works in normal mode", "persistence functional"),
                    ("Works in private/incognito", "graceful degradation"),
                    ("Works when storage full", "error handled"),
                ]
            }
        ]
    },
    {
        "name": "Backend API Validation",
        "subsections": [
            {
                "name": "Message Controller",
                "tests": [
                    ("POST /messages with valid data", "200 with message returned"),
                    ("POST /messages with empty content", "400 validation error"),
                    ("POST /messages with content >10000 chars", "400 validation error"),
                    ("POST /messages with senderId >50 chars", "400 validation error"),
                    ("GET /messages with pagination", "correct page returned"),
                    ("GET /messages/after/{seq}", "messages after sequence returned"),
                    ("GET /messages/before/{seq}", "messages before sequence returned"),
                    ("PUT /messages/{id} with valid data", "message updated"),
                    ("PUT /messages/{id} by non-owner", "403 forbidden"),
                    ("DELETE /messages/{id} by owner", "message deleted"),
                    ("DELETE /messages/{id} by non-owner", "403 forbidden"),
                    ("GET /messages/search with query", "results returned"),
                    ("GET /messages/thread/{parentId}", "replies returned"),
                    ("GET /messages/threads", "thread list returned"),
                    ("GET /messages/count", "correct count returned"),
                ]
            },
            {
                "name": "Presence Controller",
                "tests": [
                    ("POST /presence/status", "presence updated"),
                    ("POST /presence/heartbeat", "heartbeat accepted"),
                    ("GET /presence", "presence list returned"),
                    ("GET /presence/unread-count", "correct count returned"),
                ]
            },
            {
                "name": "Search Controller",
                "tests": [
                    ("GET /search with query", "results with snippets"),
                    ("GET /search with from filter", "filtered by sender"),
                    ("GET /search with before filter", "filtered by date"),
                    ("GET /search with after filter", "filtered by date"),
                    ("GET /search with invalid date", "handled gracefully"),
                ]
            },
            {
                "name": "Subscription Controller",
                "tests": [
                    ("GET /subscriptions/{userId}/tier", "tier info returned"),
                    ("POST /subscriptions/{userId}/can-sync", "authorization evaluated"),
                ]
            },
            {
                "name": "Workspace Controller",
                "tests": [
                    ("GET /workspaces", "user's workspaces returned"),
                    ("POST /workspaces/{id}/members/{userId}", "member added"),
                    ("DELETE /workspaces/{id}/members/{userId}", "member removed"),
                ]
            },
            {
                "name": "AI Controller",
                "tests": [
                    ("GET /ai/extract-status/{docId}", "status returned"),
                    ("GET /ai/extract-result/{docId}", "result returned for completed"),
                ]
            },
            {
                "name": "Webhook Controller",
                "tests": [
                    ("POST /webhooks/github with valid signature", "202 Accepted"),
                    ("POST /webhooks/github with invalid signature", "403 error"),
                ]
            }
        ]
    },
    {
        "name": "Data Consistency & Edge Cases",
        "subsections": [
            {
                "name": "Idempotency",
                "tests": [
                    ("Duplicate message with same idempotencyKey", "no duplicate created"),
                    ("Retry failed request with idempotencyKey", "same result"),
                ]
            },
            {
                "name": "Concurrent Operations",
                "tests": [
                    ("Two users edit same message", "last write wins or conflict resolution"),
                    ("Message deleted while being edited", "handle gracefully"),
                    ("Workspace deleted while active", "user redirected/notified"),
                ]
            },
            {
                "name": "Data Integrity",
                "tests": [
                    ("Sequence numbers are unique", "no duplicates in channel"),
                    ("Sequence numbers are monotonic", "always increasing"),
                    ("Timestamps are correct", "server time used"),
                ]
            }
        ]
    },
    {
        "name": "Chaos & Resilience Testing",
        "subsections": [
            {
                "name": "Backend Resilience",
                "tests": [
                    ("Database connection lost", "graceful degradation, error shown"),
                    ("Redis cache unavailable", "falls back or shows error"),
                    ("Stripe API unavailable", "checkout fails gracefully"),
                    ("GitHub unavailable", "webhook timeout handled"),
                ]
            },
            {
                "name": "Network Partition",
                "tests": [
                    ("Frontend loses backend", "shows reconnecting/error state"),
                    ("WebSocket disconnects", "attempts reconnection"),
                    ("Partial response received", "handle incomplete data"),
                ]
            }
        ]
    }
]


def set_cell_shading(cell, color):
    """Set background color for a table cell."""
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:fill'), color)
    tcPr.append(shd)


def add_checkbox_symbol(paragraph, checked=False):
    """Add a checkbox symbol to a paragraph."""
    run = paragraph.add_run("☐ " if not checked else "☑ ")
    run.font.size = Pt(11)
    return run


def create_document():
    doc = Document()
    
    # Set document margins
    for section in doc.sections:
        section.top_margin = Inches(0.75)
        section.bottom_margin = Inches(0.75)
        section.left_margin = Inches(0.75)
        section.right_margin = Inches(0.75)
    
    # ===== TITLE PAGE =====
    # Company Logo placeholder
    title = doc.add_heading('', level=0)
    run = title.add_run('SyncDoc AI')
    run.font.size = Pt(36)
    run.font.color.rgb = RGBColor(0, 82, 147)
    run.bold = True
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    subtitle = doc.add_paragraph()
    run = subtitle.add_run('Quality Assurance Testing Checklist')
    run.font.size = Pt(24)
    run.font.color.rgb = RGBColor(51, 51, 51)
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    doc.add_paragraph()
    
    # Version info
    version_para = doc.add_paragraph()
    version_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = version_para.add_run(f'Document Version: 1.0')
    run.font.size = Pt(12)
    run.font.color.rgb = RGBColor(100, 100, 100)
    
    date_para = doc.add_paragraph()
    date_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = date_para.add_run(f'Generated: {datetime.now().strftime("%B %d, %Y")}')
    run.font.size = Pt(12)
    run.font.color.rgb = RGBColor(100, 100, 100)
    
    doc.add_paragraph()
    doc.add_paragraph()
    
    # ===== TEST METADATA TABLE =====
    meta_heading = doc.add_heading('Test Execution Details', level=1)
    meta_heading.runs[0].font.color.rgb = RGBColor(0, 82, 147)
    
    meta_table = doc.add_table(rows=4, cols=4)
    meta_table.style = 'Table Grid'
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # Row 1
    meta_table.cell(0, 0).text = "Tester Name:"
    meta_table.cell(0, 1).text = ""
    meta_table.cell(0, 2).text = "Date:"
    meta_table.cell(0, 3).text = ""
    
    # Row 2
    meta_table.cell(1, 0).text = "Build Version:"
    meta_table.cell(1, 1).text = ""
    meta_table.cell(1, 2).text = "Environment:"
    meta_table.cell(1, 3).text = ""
    
    # Row 3
    meta_table.cell(2, 0).text = "Device/OS:"
    meta_table.cell(2, 1).text = ""
    meta_table.cell(2, 2).text = "Browser:"
    meta_table.cell(2, 3).text = ""
    
    # Row 4
    meta_table.cell(3, 0).text = "Test Round:"
    meta_table.cell(3, 1).text = ""
    meta_table.cell(3, 2).text = "Status:"
    meta_table.cell(3, 3).text = "☐ In Progress  ☐ Completed"
    
    # Style meta table
    for row in meta_table.rows:
        for i, cell in enumerate(row.cells):
            for paragraph in cell.paragraphs:
                paragraph.runs[0].font.size = Pt(10) if paragraph.runs else None
                if i % 2 == 0:  # Label cells
                    paragraph.runs[0].bold = True if paragraph.runs else None
                    set_cell_shading(cell, "E8E8E8")
    
    doc.add_paragraph()
    doc.add_paragraph()
    
    # ===== SUMMARY TABLE =====
    summary_heading = doc.add_heading('Test Summary by Section', level=1)
    summary_heading.runs[0].font.color.rgb = RGBColor(0, 82, 147)
    
    # Count tests per section
    summary_data = []
    total_tests = 0
    for section in SECTIONS:
        section_count = sum(len(sub["tests"]) for sub in section["subsections"])
        total_tests += section_count
        summary_data.append((section["name"], section_count))
    
    summary_table = doc.add_table(rows=len(summary_data) + 2, cols=5)
    summary_table.style = 'Table Grid'
    summary_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # Header row
    headers = ["#", "Section", "Total Tests", "Pass", "Fail"]
    for i, header in enumerate(headers):
        cell = summary_table.cell(0, i)
        cell.text = header
        set_cell_shading(cell, "005293")
        for para in cell.paragraphs:
            para.runs[0].bold = True
            para.runs[0].font.color.rgb = RGBColor(255, 255, 255)
            para.runs[0].font.size = Pt(10)
    
    # Data rows
    for idx, (name, count) in enumerate(summary_data):
        summary_table.cell(idx + 1, 0).text = str(idx + 1)
        summary_table.cell(idx + 1, 1).text = name
        summary_table.cell(idx + 1, 2).text = str(count)
        summary_table.cell(idx + 1, 3).text = ""
        summary_table.cell(idx + 1, 4).text = ""
        
        # Alternate row colors
        if idx % 2 == 1:
            for cell in summary_table.rows[idx + 1].cells:
                set_cell_shading(cell, "F5F5F5")
    
    # Total row
    total_row = len(summary_data) + 1
    summary_table.cell(total_row, 0).text = ""
    summary_table.cell(total_row, 1).text = "TOTAL"
    summary_table.cell(total_row, 2).text = str(total_tests)
    summary_table.cell(total_row, 3).text = ""
    summary_table.cell(total_row, 4).text = ""
    
    for cell in summary_table.rows[total_row].cells:
        set_cell_shading(cell, "D9E2F3")
        for para in cell.paragraphs:
            if para.runs:
                para.runs[0].bold = True
    
    # Set column widths for summary table
    for row in summary_table.rows:
        row.cells[0].width = Inches(0.4)
        row.cells[1].width = Inches(3.0)
        row.cells[2].width = Inches(0.9)
        row.cells[3].width = Inches(0.9)
        row.cells[4].width = Inches(0.9)
    
    doc.add_page_break()
    
    # ===== TEST CASES BY SECTION =====
    test_number = 0
    
    for section_idx, section in enumerate(SECTIONS):
        # Section Header
        section_heading = doc.add_heading(f'{section_idx + 1}. {section["name"]}', level=1)
        section_heading.runs[0].font.color.rgb = RGBColor(0, 82, 147)
        
        for subsection in section["subsections"]:
            # Subsection Header
            subsection_heading = doc.add_heading(subsection["name"], level=2)
            subsection_heading.runs[0].font.color.rgb = RGBColor(68, 68, 68)
            subsection_heading.runs[0].font.size = Pt(13)
            
            # Create test table
            test_table = doc.add_table(rows=len(subsection["tests"]) + 1, cols=5)
            test_table.style = 'Table Grid'
            
            # Header row
            headers = ["#", "Test Case", "Expected Result", "Pass", "Fail"]
            for i, header in enumerate(headers):
                cell = test_table.cell(0, i)
                cell.text = header
                set_cell_shading(cell, "005293")
                for para in cell.paragraphs:
                    para.runs[0].bold = True
                    para.runs[0].font.color.rgb = RGBColor(255, 255, 255)
                    para.runs[0].font.size = Pt(9)
            
            # Test case rows
            for row_idx, (test_desc, expected) in enumerate(subsection["tests"]):
                test_number += 1
                test_table.cell(row_idx + 1, 0).text = f"TC-{test_number:03d}"
                test_table.cell(row_idx + 1, 1).text = test_desc
                test_table.cell(row_idx + 1, 2).text = expected
                
                # Pass checkbox cell
                pass_cell = test_table.cell(row_idx + 1, 3)
                pass_para = pass_cell.paragraphs[0]
                pass_para.clear()
                pass_para.add_run("☐")
                pass_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
                
                # Fail checkbox cell
                fail_cell = test_table.cell(row_idx + 1, 4)
                fail_para = fail_cell.paragraphs[0]
                fail_para.clear()
                fail_para.add_run("☐")
                fail_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
                
                # Alternate row colors
                if row_idx % 2 == 1:
                    for cell in test_table.rows[row_idx + 1].cells:
                        set_cell_shading(cell, "F9F9F9")
                
                # Style text
                for cell in test_table.rows[row_idx + 1].cells:
                    for para in cell.paragraphs:
                        for run in para.runs:
                            run.font.size = Pt(9)
            
            # Set column widths
            for row in test_table.rows:
                row.cells[0].width = Inches(0.65)
                row.cells[1].width = Inches(2.8)
                row.cells[2].width = Inches(2.8)
                row.cells[3].width = Inches(0.45)
                row.cells[4].width = Inches(0.45)
            
            doc.add_paragraph()
        
        # Add page break after each major section (except last)
        if section_idx < len(SECTIONS) - 1:
            doc.add_page_break()
    
    # ===== DEFECT LOG SECTION =====
    doc.add_page_break()
    defect_heading = doc.add_heading('Defect Log', level=1)
    defect_heading.runs[0].font.color.rgb = RGBColor(0, 82, 147)
    
    defect_table = doc.add_table(rows=11, cols=5)
    defect_table.style = 'Table Grid'
    
    # Header
    defect_headers = ["Defect ID", "Test Case", "Description", "Severity", "Status"]
    for i, header in enumerate(defect_headers):
        cell = defect_table.cell(0, i)
        cell.text = header
        set_cell_shading(cell, "005293")
        for para in cell.paragraphs:
            para.runs[0].bold = True
            para.runs[0].font.color.rgb = RGBColor(255, 255, 255)
            para.runs[0].font.size = Pt(10)
    
    # Empty rows for logging defects
    for row_idx in range(1, 11):
        for col_idx in range(5):
            cell = defect_table.cell(row_idx, col_idx)
            cell.text = ""
            if row_idx % 2 == 0:
                set_cell_shading(cell, "F5F5F5")
    
    # Set column widths
    for row in defect_table.rows:
        row.cells[0].width = Inches(0.8)
        row.cells[1].width = Inches(0.8)
        row.cells[2].width = Inches(3.2)
        row.cells[3].width = Inches(0.8)
        row.cells[4].width = Inches(0.8)
    
    doc.add_paragraph()
    
    severity_note = doc.add_paragraph()
    run = severity_note.add_run("Severity Levels: ")
    run.bold = True
    run.font.size = Pt(9)
    run = severity_note.add_run("Critical (C) | High (H) | Medium (M) | Low (L)")
    run.font.size = Pt(9)
    
    status_note = doc.add_paragraph()
    run = status_note.add_run("Status: ")
    run.bold = True
    run.font.size = Pt(9)
    run = status_note.add_run("Open | In Progress | Fixed | Verified | Won't Fix")
    run.font.size = Pt(9)
    
    # ===== SIGN-OFF SECTION =====
    doc.add_page_break()
    signoff_heading = doc.add_heading('Test Sign-Off', level=1)
    signoff_heading.runs[0].font.color.rgb = RGBColor(0, 82, 147)
    
    # Summary
    summary_para = doc.add_paragraph()
    summary_para.add_run("Test Execution Summary").bold = True
    
    signoff_summary = doc.add_table(rows=5, cols=2)
    signoff_summary.style = 'Table Grid'
    
    summary_rows = [
        ("Total Test Cases:", str(total_tests)),
        ("Tests Passed:", ""),
        ("Tests Failed:", ""),
        ("Pass Rate:", ""),
        ("Blockers/Critical Defects:", ""),
    ]
    
    for idx, (label, value) in enumerate(summary_rows):
        signoff_summary.cell(idx, 0).text = label
        signoff_summary.cell(idx, 0).paragraphs[0].runs[0].bold = True
        set_cell_shading(signoff_summary.cell(idx, 0), "E8E8E8")
        signoff_summary.cell(idx, 1).text = value
    
    for row in signoff_summary.rows:
        row.cells[0].width = Inches(2.0)
        row.cells[1].width = Inches(2.0)
    
    doc.add_paragraph()
    doc.add_paragraph()
    
    # Approval section
    approval_para = doc.add_paragraph()
    approval_para.add_run("Approval Sign-Off").bold = True
    
    approval_table = doc.add_table(rows=4, cols=4)
    approval_table.style = 'Table Grid'
    
    # Header
    approval_headers = ["Role", "Name", "Signature", "Date"]
    for i, header in enumerate(approval_headers):
        cell = approval_table.cell(0, i)
        cell.text = header
        set_cell_shading(cell, "005293")
        for para in cell.paragraphs:
            para.runs[0].bold = True
            para.runs[0].font.color.rgb = RGBColor(255, 255, 255)
    
    # Roles
    roles = ["QA Lead", "Development Lead", "Product Owner"]
    for idx, role in enumerate(roles):
        approval_table.cell(idx + 1, 0).text = role
        approval_table.cell(idx + 1, 0).paragraphs[0].runs[0].bold = True
    
    for row in approval_table.rows:
        row.cells[0].width = Inches(1.5)
        row.cells[1].width = Inches(1.8)
        row.cells[2].width = Inches(1.8)
        row.cells[3].width = Inches(1.2)
    
    doc.add_paragraph()
    doc.add_paragraph()
    
    # Notes section
    notes_para = doc.add_paragraph()
    notes_para.add_run("Additional Notes / Comments:").bold = True
    
    # Add lines for notes
    for _ in range(5):
        line = doc.add_paragraph()
        line.add_run("_" * 95)
        line.runs[0].font.color.rgb = RGBColor(200, 200, 200)
    
    doc.add_paragraph()
    
    # Footer note
    footer = doc.add_paragraph()
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = footer.add_run("This document is confidential and intended for internal use only.")
    run.font.size = Pt(9)
    run.font.color.rgb = RGBColor(128, 128, 128)
    run.italic = True
    
    return doc


if __name__ == "__main__":
    print("Generating Testing Checklist Word Document...")
    doc = create_document()
    output_path = "/Users/ankitkumar/Documents/building-something/syncdoc-ai/TESTING_CHECKLIST.docx"
    doc.save(output_path)
    print(f"Document saved to: {output_path}")
