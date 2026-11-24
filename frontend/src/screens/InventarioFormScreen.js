import React, { useState } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
} from 'react-native';
import api from '../services/api';

export default function InventarioFormScreen({ route, navigation }) {
    const { mode, inventario, onSuccess } = route.params || {};
    const isEdit = mode === 'edit';

    const [nome, setNome] = useState(inventario?.nome || '');
    const [descricao, setDescricao] = useState(inventario?.descricao || '');
    const [salvando, setSalvando] = useState(false);
    const [erro, setErro] = useState('');

    async function handleSalvar() {
        if (!nome.trim()) {
            setErro('O nome do inventário é obrigatório.');
            return;
        }

        const payload = {
            nome: nome.trim(),
            descricao: descricao.trim() || null,
        };

        try {
            setSalvando(true);
            setErro('');

            if (isEdit && inventario?.id) {
                await api.put(`/api/inventarios/${inventario.id}`, payload);
                onSuccess?.('Inventário atualizado com sucesso!');
            } else {
                await api.post('/api/inventarios', payload);
                onSuccess?.('Inventário cadastrado com sucesso!');
            }

            navigation.goBack();
        } catch (error) {
            const mensagemBackend =
                typeof error.response?.data === 'string' && error.response.data.trim()
                    ? error.response.data
                    : 'Não foi possível salvar o inventário.';
            setErro(mensagemBackend);
        } finally {
            setSalvando(false);
        }
    }

    return (
        <View style={styles.container}>
            <Text style={styles.titulo}>
                {isEdit ? 'Editar Inventário' : 'Novo Inventário'}
            </Text>

            {erro ? (
                <View style={styles.bannerErro}>
                    <Text style={styles.textoErro}>{erro}</Text>
                </View>
            ) : null}

            <Text style={styles.label}>Nome *</Text>
            <TextInput
                style={styles.input}
                placeholder="Ex: Inventário Principal"
                placeholderTextColor="#71717A"
                value={nome}
                onChangeText={(texto) => {
                    setNome(texto);
                    if (erro) setErro('');
                }}
                maxLength={100}
            />

            <Text style={styles.label}>Descrição (opcional)</Text>
            <TextInput
                style={[styles.input, styles.inputMultiline]}
                placeholder="Ex: Minhas skins favoritas de rifle"
                placeholderTextColor="#71717A"
                value={descricao}
                onChangeText={setDescricao}
                maxLength={255}
                multiline
                numberOfLines={3}
            />

            <TouchableOpacity
                style={[styles.botaoSalvar, salvando && styles.botaoDesabilitado]}
                onPress={handleSalvar}
                disabled={salvando}
            >
                <Text style={styles.textoBotao}>
                    {salvando ? 'Salvando...' : 'Salvar'}
                </Text>
            </TouchableOpacity>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#121214',
        padding: 16,
    },
    titulo: {
        color: '#FFFFFF',
        fontSize: 22,
        fontWeight: 'bold',
        marginBottom: 16,
    },
    bannerErro: {
        backgroundColor: '#7F1D1D',
        borderColor: '#EF4444',
        borderWidth: 1,
        borderRadius: 8,
        padding: 10,
        marginBottom: 12,
    },
    textoErro: {
        color: '#FECACA',
        fontSize: 13,
        fontWeight: '500',
    },
    label: {
        color: '#E4E4E7',
        fontSize: 14,
        fontWeight: '600',
        marginBottom: 6,
        marginTop: 8,
    },
    input: {
        backgroundColor: '#1E1E24',
        color: '#FFFFFF',
        borderRadius: 8,
        paddingHorizontal: 12,
        paddingVertical: 10,
        borderWidth: 1,
        borderColor: '#27272A',
        marginBottom: 10,
    },
    inputMultiline: {
        minHeight: 80,
        textAlignVertical: 'top',
    },
    botaoSalvar: {
        backgroundColor: '#2563EB',
        paddingVertical: 12,
        borderRadius: 8,
        alignItems: 'center',
        marginTop: 16,
    },
    botaoDesabilitado: {
        opacity: 0.7,
    },
    textoBotao: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '600',
    },
});