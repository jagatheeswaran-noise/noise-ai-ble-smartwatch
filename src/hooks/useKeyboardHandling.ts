import { useEffect, useCallback } from 'react';
import { Keyboard } from 'react-native';

interface UseKeyboardHandlingProps {
  onKeyboardShow?: () => void;
  onKeyboardHide?: () => void;
  autoScrollOnShow?: boolean;
  scrollToBottom?: () => void;
}

interface UseKeyboardHandlingReturn {
  dismissKeyboard: () => void;
}

export const useKeyboardHandling = ({
  onKeyboardShow,
  onKeyboardHide,
  autoScrollOnShow = true,
  scrollToBottom,
}: UseKeyboardHandlingProps = {}): UseKeyboardHandlingReturn => {

  const handleKeyboardShow = useCallback(() => {
    console.log('⌨️ Keyboard shown');
    onKeyboardShow?.();
    
    if (autoScrollOnShow && scrollToBottom) {
      // Delay scroll to allow keyboard animation to complete
      setTimeout(() => {
        scrollToBottom();
      }, 100);
    }
  }, [onKeyboardShow, autoScrollOnShow, scrollToBottom]);

  const handleKeyboardHide = useCallback(() => {
    console.log('⌨️ Keyboard hidden');
    onKeyboardHide?.();
  }, [onKeyboardHide]);

  const dismissKeyboard = useCallback(() => {
    Keyboard.dismiss();
  }, []);

  useEffect(() => {
    const keyboardDidShowListener = Keyboard.addListener('keyboardDidShow', handleKeyboardShow);
    const keyboardDidHideListener = Keyboard.addListener('keyboardDidHide', handleKeyboardHide);

    return () => {
      keyboardDidShowListener?.remove();
      keyboardDidHideListener?.remove();
    };
  }, [handleKeyboardShow, handleKeyboardHide]);

  return {
    dismissKeyboard,
  };
};
