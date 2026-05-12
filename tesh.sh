#!/bin/bash

set -e

# ============================================================
# Git Stage Allowed Files Only + Restore Everything Else
# Generate + Preview Commit Message From Created/Modified/Deleted Files
# Commit + Push
# ============================================================
#
# Allowed extensions:
#   .java
#   .yml
#   .yaml
#   .properties
#
# What this script does:
#   1. Shows git status
#   2. Finds changed/untracked/deleted files
#   3. Stages only allowed file types
#   4. Restores tracked files with disallowed extensions
#   5. Deletes untracked files with disallowed extensions except tesh.sh
#   6. Creates a commit message based on created/updated/deleted files
#   7. Lets you preview the commit message before continuing
#   8. Commits the staged files
#   9. Shows a complete push summary
#   10. Asks if you want to push
#   11. Pushes to remote with upstream
#
# Example generated commit message:
#   Created ProjectEntity, updated ProjectServiceImpl, and deleted OldProjectConfig
#
# ============================================================

ALLOWED_EXTENSIONS_REGEX='\.(java|yml|yaml|properties)$'
PROTECTED_UNTRACKED_FILE="tesh.sh"

FILES_TO_ADD=()
TRACKED_FILES_TO_RESTORE=()
UNTRACKED_FILES_TO_DELETE=()

CREATED_NAMES=()
UPDATED_NAMES=()
DELETED_NAMES=()

PREVIEW_ONLY=false
PUSH_SKIP_CONFIRM=false
GIT_PUSH_ONLY=false
INSTALL_ALIAS=false

# ============================================================
# Usage
# ============================================================

usage() {
  echo ""
  echo "Usage:"
  echo "  ./git-smart-add.sh [options]"
  echo ""
  echo "Options:"
  echo "  --preview, -p      Preview generated commit message only. Does not stage, restore, delete, commit, or push files."
  echo "  --p-skip, -ps      Preview commit message, allow edit, then stage, restore, delete, commit, and push."
  echo "  --p-skill          Same as --p-skip."
  echo "  --git-push         Push current branch to origin without asking."
  echo "  --install-alias    Create an alias using this file name without extension."
  echo "  --help, -h         Show help."
  echo ""
}

# ============================================================
# Parse args
# ============================================================

while [[ $# -gt 0 ]]; do
  case "$1" in
    --preview|-p)
      PREVIEW_ONLY=true
      shift
      ;;
    --p-skip|-ps|--p-skill)
      PUSH_SKIP_CONFIRM=true
      shift
      ;;
    --git-push)
      GIT_PUSH_ONLY=true
      shift
      ;;
    --install-alias)
      INSTALL_ALIAS=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Error: Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

# ============================================================
# Install alias
# ============================================================

if [ "$INSTALL_ALIAS" = true ]; then
  SCRIPT_ABSOLUTE_PATH="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
  SCRIPT_FILE_NAME="$(basename "$0")"
  ALIAS_NAME="${SCRIPT_FILE_NAME%.*}"
  SHELL_CONFIG="$HOME/.zshrc"
  ALIAS_LINE="alias $ALIAS_NAME=$SCRIPT_ABSOLUTE_PATH"

  touch "$SHELL_CONFIG"

  if grep -q "^alias $ALIAS_NAME=" "$SHELL_CONFIG"; then
    sed -i.bak "/^alias $ALIAS_NAME=/d" "$SHELL_CONFIG"
  fi

  echo "$ALIAS_LINE" >> "$SHELL_CONFIG"

  echo ""
  echo "Alias installed:"
  echo ""
  echo "$ALIAS_LINE"
  echo ""
  echo "Run this command to reload your shell:"
  echo ""
  echo "source ~/.zshrc"
  echo ""

  exit 0
fi

# ============================================================
# Safety check: must be inside a git repo
# ============================================================

if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
  echo "Error: This is not inside a git repository."
  exit 1
fi

# ============================================================
# Helper functions
# ============================================================

is_allowed_file() {
  local file="$1"
  [[ "$file" =~ $ALLOWED_EXTENSIONS_REGEX ]]
}

is_protected_untracked_file() {
  local file="$1"
  local filename

  filename=$(basename "$file")

  [[ "$filename" == "$PROTECTED_UNTRACKED_FILE" ]]
}

get_name_without_extension() {
  local file="$1"
  local filename

  filename=$(basename "$file")

  filename="${filename%.java}"
  filename="${filename%.yml}"
  filename="${filename%.yaml}"
  filename="${filename%.properties}"

  echo "$filename"
}

contains_value() {
  local value="$1"
  shift

  for existing in "$@"; do
    if [ "$existing" = "$value" ]; then
      return 0
    fi
  done

  return 1
}

add_unique_created_name() {
  local name="$1"

  if ! contains_value "$name" "${CREATED_NAMES[@]}"; then
    CREATED_NAMES+=("$name")
  fi
}

add_unique_updated_name() {
  local name="$1"

  if ! contains_value "$name" "${UPDATED_NAMES[@]}"; then
    UPDATED_NAMES+=("$name")
  fi
}

add_unique_deleted_name() {
  local name="$1"

  if ! contains_value "$name" "${DELETED_NAMES[@]}"; then
    DELETED_NAMES+=("$name")
  fi
}

join_with_and() {
  local items=("$@")
  local count="${#items[@]}"
  local result=""

  if [ "$count" -eq 0 ]; then
    echo ""
    return
  fi

  if [ "$count" -eq 1 ]; then
    echo "${items[0]}"
    return
  fi

  for i in "${!items[@]}"; do
    if [ "$i" -eq 0 ]; then
      result="${items[$i]}"
    elif [ "$i" -eq $((count - 1)) ]; then
      result="$result and ${items[$i]}"
    else
      result="$result, ${items[$i]}"
    fi
  done

  echo "$result"
}

print_list_or_none() {
  local items=("$@")

  if [ "${#items[@]}" -eq 0 ]; then
    echo "None"
  else
    printf '%s\n' "${items[@]}"
  fi
}

# ============================================================
# Current branch
# ============================================================

CURRENT_BRANCH=$(git branch --show-current)

if [ -z "$CURRENT_BRANCH" ]; then
  echo "Error: Could not detect current git branch."
  exit 1
fi

# ============================================================
# Git push only
# ============================================================

if [ "$GIT_PUSH_ONLY" = true ]; then
  echo ""
  echo "Pushing current branch:"
  echo ""
  echo "git push --set-upstream origin $CURRENT_BRANCH"
  echo ""

  git push --set-upstream origin "$CURRENT_BRANCH"

  echo ""
  echo "Done. Final git status:"
  echo ""

  git status --short

  exit 0
fi

# ============================================================
# Current status
# ============================================================

echo ""
echo "Current git status:"
echo ""

git status --short

echo ""
echo "Finding files..."
echo ""

# ============================================================
# Read git status
# ============================================================

while IFS= read -r line; do
  status="${line:0:2}"
  file="${line:3}"

  # Skip empty lines
  if [ -z "$file" ]; then
    continue
  fi

  # Handle renamed files shown like:
  # R  old/path/File.java -> new/path/File.java
  if [[ "$file" == *" -> "* ]]; then
    file="${file##* -> }"
  fi

  if is_allowed_file "$file"; then
    FILES_TO_ADD+=("$file")

    name=$(get_name_without_extension "$file")

    if [[ "$status" == "??" || "$status" == "A "* || "$status" == *"A" ]]; then
      add_unique_created_name "$name"
    elif [[ "${status:0:1}" == "D" || "${status:1:1}" == "D" ]]; then
      add_unique_deleted_name "$name"
    else
      add_unique_updated_name "$name"
    fi
  else
    if [[ "$status" == "??" ]]; then
      if ! is_protected_untracked_file "$file"; then
        UNTRACKED_FILES_TO_DELETE+=("$file")
      fi
    else
      TRACKED_FILES_TO_RESTORE+=("$file")
    fi
  fi

done < <(git status --short)

# ============================================================
# Show what will happen
# ============================================================

echo ""
echo "Allowed files that will be staged:"
echo ""

print_list_or_none "${FILES_TO_ADD[@]}"

echo ""
echo "Tracked disallowed files that will be restored:"
echo ""

print_list_or_none "${TRACKED_FILES_TO_RESTORE[@]}"

echo ""
echo "Untracked disallowed files that will be deleted:"
echo ""

print_list_or_none "${UNTRACKED_FILES_TO_DELETE[@]}"

echo ""
echo "Allowed created files for commit message:"
echo ""

print_list_or_none "${CREATED_NAMES[@]}"

echo ""
echo "Allowed updated files for commit message:"
echo ""

print_list_or_none "${UPDATED_NAMES[@]}"

echo ""
echo "Allowed deleted files for commit message:"
echo ""

print_list_or_none "${DELETED_NAMES[@]}"

# ============================================================
# Create commit message
# ============================================================

CREATED_TEXT=$(join_with_and "${CREATED_NAMES[@]}")
UPDATED_TEXT=$(join_with_and "${UPDATED_NAMES[@]}")
DELETED_TEXT=$(join_with_and "${DELETED_NAMES[@]}")

MESSAGE_PARTS=()

if [ -n "$CREATED_TEXT" ]; then
  MESSAGE_PARTS+=("created $CREATED_TEXT")
fi

if [ -n "$UPDATED_TEXT" ]; then
  MESSAGE_PARTS+=("updated $UPDATED_TEXT")
fi

if [ -n "$DELETED_TEXT" ]; then
  MESSAGE_PARTS+=("deleted $DELETED_TEXT")
fi

COMMIT_MESSAGE=""

if [ "${#MESSAGE_PARTS[@]}" -eq 1 ]; then
  COMMIT_MESSAGE="${MESSAGE_PARTS[0]}"
  COMMIT_MESSAGE="$(tr '[:lower:]' '[:upper:]' <<< "${COMMIT_MESSAGE:0:1}")${COMMIT_MESSAGE:1}"
elif [ "${#MESSAGE_PARTS[@]}" -eq 2 ]; then
  COMMIT_MESSAGE="${MESSAGE_PARTS[0]} and ${MESSAGE_PARTS[1]}"
  COMMIT_MESSAGE="$(tr '[:lower:]' '[:upper:]' <<< "${COMMIT_MESSAGE:0:1}")${COMMIT_MESSAGE:1}"
elif [ "${#MESSAGE_PARTS[@]}" -eq 3 ]; then
  COMMIT_MESSAGE="${MESSAGE_PARTS[0]}, ${MESSAGE_PARTS[1]}, and ${MESSAGE_PARTS[2]}"
  COMMIT_MESSAGE="$(tr '[:lower:]' '[:upper:]' <<< "${COMMIT_MESSAGE:0:1}")${COMMIT_MESSAGE:1}"
else
  COMMIT_MESSAGE=""
fi

# ============================================================
# Preview commit message
# ============================================================

echo ""
echo "Generated commit message preview:"
echo ""

if [ -z "$COMMIT_MESSAGE" ]; then
  echo "No commit message generated because no allowed files were found."
else
  echo "------------------------------------------------------------"
  echo "$COMMIT_MESSAGE"
  echo "------------------------------------------------------------"
  echo ""
  echo "Git commands:"
  echo "git commit -m \"$COMMIT_MESSAGE\""
  echo "git push --set-upstream origin $CURRENT_BRANCH"
fi

if [ "$PREVIEW_ONLY" = true ]; then
  echo ""
  echo "Preview only mode. No files were staged, restored, deleted, committed, or pushed."
  exit 0
fi

if [ -z "$COMMIT_MESSAGE" ]; then
  echo ""
  echo "No allowed files found. Nothing to commit or push."
  exit 0
fi

if [ "$PUSH_SKIP_CONFIRM" = true ]; then
  echo ""
  read -p "Use this commit message? y = yes, n = edit: " COMMIT_MESSAGE_CONFIRM

  if [[ "$COMMIT_MESSAGE_CONFIRM" != "y" && "$COMMIT_MESSAGE_CONFIRM" != "yes" ]]; then
    echo ""
    read -p "Enter commit message: " CUSTOM_COMMIT_MESSAGE

    if [ -z "$CUSTOM_COMMIT_MESSAGE" ]; then
      echo "Commit message cannot be empty."
      exit 1
    fi

    COMMIT_MESSAGE="$CUSTOM_COMMIT_MESSAGE"

    echo ""
    echo "Updated commit message:"
    echo "------------------------------------------------------------"
    echo "$COMMIT_MESSAGE"
    echo "------------------------------------------------------------"
  fi

  echo ""
  echo "--p-skip enabled. Continuing with commit and push."
else
  echo ""
  read -p "Use this commit message and commit? This may delete untracked disallowed files. (y/n): " CONFIRM

  if [[ "$CONFIRM" != "y" && "$CONFIRM" != "yes" ]]; then
    echo "Cancelled. No files were changed."
    exit 0
  fi
fi

# ============================================================
# Stage allowed files
# ============================================================

if [ "${#FILES_TO_ADD[@]}" -gt 0 ]; then
  echo ""
  echo "Staging allowed files..."
  echo ""

  for file in "${FILES_TO_ADD[@]}"; do
    git add "$file"
  done
fi

# ============================================================
# Restore tracked disallowed files
# ============================================================

if [ "${#TRACKED_FILES_TO_RESTORE[@]}" -gt 0 ]; then
  echo ""
  echo "Restoring tracked disallowed files..."
  echo ""

  for file in "${TRACKED_FILES_TO_RESTORE[@]}"; do
    git restore "$file"
  done
fi

# ============================================================
# Delete untracked disallowed files except tesh.sh
# ============================================================

if [ "${#UNTRACKED_FILES_TO_DELETE[@]}" -gt 0 ]; then
  echo ""
  echo "Deleting untracked disallowed files..."
  echo ""

  for file in "${UNTRACKED_FILES_TO_DELETE[@]}"; do
    rm -rf "$file"
  done
fi

# ============================================================
# Commit
# ============================================================

echo ""
echo "Checking staged files..."
echo ""

if git diff --cached --quiet; then
  echo "No staged changes found. Nothing to commit or push."
  exit 0
fi

echo ""
echo "Committing..."
echo ""

git commit -m "$COMMIT_MESSAGE"

# ============================================================
# Push summary
# ============================================================

echo ""
echo "Complete push summary:"
echo ""
echo "Files added to commit:"
echo ""

print_list_or_none "${FILES_TO_ADD[@]}"

echo ""
echo "Created:"
echo ""

print_list_or_none "${CREATED_NAMES[@]}"

echo ""
echo "Modified:"
echo ""

print_list_or_none "${UPDATED_NAMES[@]}"

echo ""
echo "Deleted:"
echo ""

print_list_or_none "${DELETED_NAMES[@]}"

echo ""
echo "Commit message:"
echo ""

echo "------------------------------------------------------------"
echo "$COMMIT_MESSAGE"
echo "------------------------------------------------------------"

echo ""
echo "Push command:"
echo ""

echo "git push --set-upstream origin $CURRENT_BRANCH"

# ============================================================
# Push
# ============================================================

if [ "$PUSH_SKIP_CONFIRM" = false ]; then
  echo ""
  read -p "Would you like to push? y = yes, n = no: " PUSH_CONFIRM

  if [[ "$PUSH_CONFIRM" != "y" && "$PUSH_CONFIRM" != "yes" ]]; then
    echo "Commit created. Push skipped."
    exit 0
  fi
else
  echo ""
  echo "--p-skip enabled. Skipping push confirmation."
fi

echo ""
echo "Pushing..."
echo ""

git push --set-upstream origin "$CURRENT_BRANCH"

# ============================================================
# Final output
# ============================================================

echo ""
echo "Done. Final git status:"
echo ""

git status --short